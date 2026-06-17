package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import com.hcy.quant_core.modules.statarb.config.StatArbProperties;
import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.model.direction.StatArbDirection;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class StatArbScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatArbScheduler.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IStatArbUseCase statArbUseCase;
	private final StatArbSignalPersistencePort persistencePort;
	private final IAlertPublisher alertPublisher;
	private final StatArbProperties props;
	private final SignalAlertPersistencePort signalAlertPersistencePort;

	// Edge Trigger 狀態：每個交易對獨立追蹤
	private final Map<String, StatArbDirection> lastDirectionByPair = new ConcurrentHashMap<>();

	// 每次 fork 的計算結果容器（private record，不對外暴露）
	private record PairResult(StatArbSignalRecord signal, StatArbProperties.PairConfig pair) {}

	private final ReentrantLock schedulerLock = new ReentrantLock();                    // 防重入
	private final AtomicInteger consecutiveSkips = new AtomicInteger(0);        //連續 skip 次數
	private final Counter skipCounter;
	//總 skip 次數

	public StatArbScheduler(IStatArbUseCase statArbUseCase,
		StatArbSignalPersistencePort persistencePort,
		IAlertPublisher alertPublisher,
		StatArbProperties props,
		MeterRegistry meterRegistry,
		SignalAlertPersistencePort signalAlertPersistencePort) {
		this.statArbUseCase = statArbUseCase;
		this.persistencePort = persistencePort;
		this.alertPublisher = alertPublisher;
		this.props = props;
		this.signalAlertPersistencePort = signalAlertPersistencePort;
		this.skipCounter = Counter.builder("statarb.scheduler.skip.total")
			.description("Total times StatArb scheduler was skipped due to lock contention")
			.register(meterRegistry);
		Gauge.builder("statarb.scheduler.consecutive.skips",
				consecutiveSkips, AtomicInteger::get)
			.description("Current consecutive skip count for StatArb scheduler")
			.register(meterRegistry);
	}

	@Scheduled(cron = "${app.scheduler.statarb.cron:-}")
	public void calculateAndPublish() {
		if (!schedulerLock.tryLock()) {
			skipCounter.increment();
			int count = consecutiveSkips.incrementAndGet();
			LOGGER.warn(
				"StatArb scheduler skipped, previous invocation still running (consecutive: {})",
				count);
			if (count >= 3) {
				LOGGER.error(
					"StatArb consecutive skips = {}, execution time exceeds cron interval, " +
						"investigate!",
					count);
			}
			return;
		}
		try {
			// 成功取得鎖 → 連續 skip 的次數 歸零
			consecutiveSkips.set(0);

			try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
				List<StructuredTaskScope.Subtask<PairResult>> subtasks = props.pairs().stream()
					.map(pair -> scope.fork(() -> calculatePair(pair)))
					.toList();

				scope.join();

				subtasks.stream()
					.map(StructuredTaskScope.Subtask::get)
					.filter(r -> r.signal() != null)
					.forEach(r -> {
						persistencePort.save(r.signal());
						handleSignal(r.signal(), r.pair());
					});

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.warn("StatArb scheduler interrupted");
			}

		} finally {
			//釋放 schedulerLock.tryLock()
			schedulerLock.unlock();
		}
	}

	private PairResult calculatePair(StatArbProperties.PairConfig pair) {
		try {
			//			Thread.sleep(3000); // 模擬慢速執行
			StatArbParams params = new StatArbParams(
				pair.zscoreThreshold(),
				pair.zscoreExitThreshold(),
				pair.zscoreStopLossThreshold(),
				pair.lookbackSize()
			);
			StatArbSignalRecord signal = statArbUseCase.calculate(
				pair.symbolA(), pair.symbolB(), params
			);
			return new PairResult(signal, pair);
		} catch (Exception e) {
			LOGGER.warn("Calculate failed: {}/{}", pair.symbolA(), pair.symbolB(), e);
			return new PairResult(null, pair);
		}
	}

	private void handleSignal(StatArbSignalRecord signal, StatArbProperties.PairConfig pair) {
		String pairKey = pair.symbolA() + "_" + pair.symbolB();
		StatArbDirection lastActive =
			lastDirectionByPair.getOrDefault(pairKey, StatArbDirection.HOLD);
		StatArbDirection current = StatArbDirection.valueOf(signal.direction());

		boolean wasInPosition = isPositionDirection(lastActive);    //上一次是否有持倉
		boolean isInPosition = isPositionDirection(current);        //這一次是否要持倉

		// OPEN_LONG_B -> OPEN_SHORT_B || OPEN_SHORT_B-> OPEN_LONG_B
		boolean isDirectFlip = wasInPosition && isInPosition && current != lastActive;

		LOGGER.info("pairKey:{},上一次是否有持倉:{}, 這一次是否要持倉:{}, 是否直翻:{}", pairKey,
			wasInPosition,
			isInPosition, isDirectFlip);

		if (isDirectFlip) {
			// EXIT 舊方向，ENTRY 新方向
			alertPublisher.publish(buildPayload(signal, lastActive, AlertType.EXIT));
			saveSignalAlert(signal, lastActive, AlertType.EXIT);
 
			alertPublisher.publish(buildPayload(signal, current, AlertType.ENTRY));
			saveSignalAlert(signal, current, AlertType.ENTRY);
		} else if (!wasInPosition && isInPosition) {
			// 無倉 → 開倉
			alertPublisher.publish(buildPayload(signal, current, AlertType.ENTRY));
			saveSignalAlert(signal, current, AlertType.ENTRY);
		} else if (wasInPosition && !isInPosition && current != StatArbDirection.HOLD) {
			// 持倉 → 平倉，STOP_LOSS / EXIT 要平倉、HOLD不平倉
			alertPublisher.publish(buildPayload(signal, lastActive, AlertType.EXIT));
			saveSignalAlert(signal, lastActive, AlertType.EXIT);
		} else {
			// 無變化：NONE, 不寫入 signal_alert
			alertPublisher.publish(buildPayload(signal, current, AlertType.NONE));
		}

		if (current != StatArbDirection.HOLD) {
			lastDirectionByPair.put(pairKey, current);
		}
	}

	private boolean isPositionDirection(StatArbDirection direction) {
		return direction == StatArbDirection.OPEN_LONG_B
			|| direction == StatArbDirection.OPEN_SHORT_B;
	}

	private SignalAlertPayload buildPayload(StatArbSignalRecord signal,
		StatArbDirection direction, AlertType alertType) {
		String symbolPair = signal.symbolA() + "/" + signal.symbolB();
		return new SignalAlertPayload(
			"STAT_ARB",
			symbolPair,
			direction,
			signal.zScore(),
			alertType,
			String.format("[%s] Z-Score: %.4f | Direction: %s", symbolPair, signal.zScore(),
				direction),
			signal.signalAt()
		);
	}

	private void saveSignalAlert(StatArbSignalRecord signal,
		StatArbDirection direction, AlertType alertType) {
		signalAlertPersistencePort.save(new SignalAlertRecord(
			"STAT_ARB",
			direction.name(),
			alertType.name(),
			signal.symbolA(),
			signal.symbolB(),
			signal.symbolAPrice(),
			signal.symbolBPrice(),
			signal.signalAt()
		));
	}
}

package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.model.direction.StatArbDirection;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StatArbScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatArbScheduler.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IStatArbUseCase statArbUseCase;
	private final StatArbSignalPersistencePort persistencePort;
	private final IAlertPublisher alertPublisher;

	// Edge Trigger 狀態追蹤
	private StatArbDirection lastActiveDirection = StatArbDirection.HOLD;

	public StatArbScheduler(IStatArbUseCase statArbUseCase,
		StatArbSignalPersistencePort persistencePort,
		IAlertPublisher alertPublisher) {
		this.statArbUseCase = statArbUseCase;
		this.persistencePort = persistencePort;
		this.alertPublisher = alertPublisher;
	}

	// 每 30 秒執行一次
	@Scheduled(fixedDelay = 30_000)
	public void calculateAndPublish() {
		StatArbSignalRecord signal = statArbUseCase.calculate(
			"BTCUSDT", "ETHUSDT", StatArbParams.defaults()
		);

		if (signal == null) {
			LOGGER.info("數據不足 WebSocket 剛啟動還在累積");
			return;
		}

		persistencePort.save(signal);

		StatArbDirection currentDirection = StatArbDirection.valueOf(signal.direction());

		boolean wasInPosition = isPositionDirection(lastActiveDirection);    //上一次是否有持倉
		boolean isInPosition = isPositionDirection(currentDirection);        //這一次是否要持倉

		// OPEN_LONG_B -> OPEN_SHORT_B || OPEN_SHORT_B-> OPEN_LONG_B
		boolean isDirectFlip = wasInPosition && isInPosition
			&& currentDirection != lastActiveDirection;

		if (isDirectFlip) {
			// EXIT 舊方向，ENTRY 新方向
			alertPublisher.publish(buildPayload(signal, lastActiveDirection,
				AlertType.EXIT));
			alertPublisher.publish(buildPayload(signal, currentDirection,
				AlertType.ENTRY));
		} else if (!wasInPosition && isInPosition) {
			// 無倉 → 開倉
			alertPublisher.publish(buildPayload(signal, currentDirection,
				AlertType.ENTRY));
		} else if (wasInPosition && !isInPosition) {
			// 持倉 → 平倉：direction 用 lastActiveDirection，不用 HOLD
			alertPublisher.publish(buildPayload(signal, lastActiveDirection,
				AlertType.EXIT));
		} else {
			// 無變化：NONE
			alertPublisher.publish(buildPayload(signal, currentDirection,
				AlertType.NONE));
		}

		lastActiveDirection = currentDirection;
	}

	private boolean isPositionDirection(StatArbDirection direction) {
		return direction == StatArbDirection.OPEN_LONG_B
			|| direction == StatArbDirection.OPEN_SHORT_B;
	}

	private SignalAlertPayload buildPayload(StatArbSignalRecord signal,
		StatArbDirection direction,
		AlertType alertType) {
		return new SignalAlertPayload(
			"STAT_ARB",
			direction,
			signal.zScore(),
			alertType,
			String.format("Z-Score: %.4f | Direction: %s", signal.zScore(), direction),
			signal.signalAt()
		);
	}
}

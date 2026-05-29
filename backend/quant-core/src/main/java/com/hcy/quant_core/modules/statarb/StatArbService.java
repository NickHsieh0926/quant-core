package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.util.CacheKeyConstants;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.statarb.calculator.ZScoreCalculator;
import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatArbService implements IStatArbUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatArbService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final StatArbSignalPersistencePort persistencePort;
	private final StringRedisTemplate redisTemplate;
	private final ZScoreCalculator zScoreCalculator;

	public StatArbService(StatArbSignalPersistencePort persistencePort,
		StringRedisTemplate redisTemplate,
		ZScoreCalculator zScoreCalculator) {
		this.persistencePort = persistencePort;
		this.redisTemplate = redisTemplate;
		this.zScoreCalculator = zScoreCalculator;
	}

	@Override
	public List<StatArbSignalRecord> getRecentSignals(int limit) {
		TRACE.message("getRecentSignals limit:{}", limit);
		return persistencePort.findLatest(limit);
	}

	@Override
	public StatArbSignalRecord calculate(String symbolA, String symbolB, StatArbParams params) {
		List<BigDecimal> pricesA = getClosedPrices(symbolA, params.lookbackSize());
		List<BigDecimal> pricesB = getClosedPrices(symbolB, params.lookbackSize());

		if (pricesA.size() < params.lookbackSize() || pricesB.size() < params.lookbackSize()) {
			LOGGER.info("數據不足，等待 WebSocket 累積：A.Size={} B.Size={} need={}", pricesA.size(),
				pricesB.size(), params.lookbackSize());
			return null;
		}

		double zScore = zScoreCalculator.calculate(pricesA, pricesB);

		String direction = switch (Double.compare(Math.abs(zScore), params.entryThreshold())) {
			case 1 -> zScore > 0 ? "OPEN_LONG_B" : "OPEN_SHORT_B";  // |z| > 2.0 || |z| < -2.0
			default -> Math.abs(zScore) < params.exitThreshold() ? "CLOSE" : "HOLD";
		};

		boolean triggered = Math.abs(zScore) >= params.entryThreshold();

		// 最新 Z-Score 快取
		redisTemplate.opsForValue().set(
			CacheKeyConstants.zScore(symbolA, symbolB),
			String.valueOf(zScore)
		);

		return new StatArbSignalRecord(symbolA, symbolB, zScore, direction, triggered,
			LocalDateTime.now());
	}

	private List<BigDecimal> getClosedPrices(String symbol, int size) {
		// 取 Redis List 的前 size 筆
		List<String> raw = redisTemplate.opsForList()
			.range(CacheKeyConstants.closedPriceList(symbol), 0, size - 1);
		if (raw == null)
			return List.of();
		return raw.stream().map(BigDecimal::new).toList();
	}


}

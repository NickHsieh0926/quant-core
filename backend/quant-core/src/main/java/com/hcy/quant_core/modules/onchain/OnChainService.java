package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;
import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OnChainService implements IOnChainUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnChainService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final OnChainMetricsPersistencePort persistencePort;

	public OnChainService(OnChainMetricsPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public List<OnChainMetricsRecord> getLatestMetrics(int limit) {
		return persistencePort.findLatest(limit);
	}

	@Override
	public List<OnChainMetricsRecord> getAllMetrics() {
		return persistencePort.findAll();
	}


	@Override
	public OnChainSignalRecord calculateSignal() {
		OnChainMetricsRecord latest = persistencePort.findLatestOne();
		if (latest == null)
			return null;

		int score = calculateCompositeScore(latest.fearGreedIndex(), latest.btcExchangeFlow());

		String direction = score >= 60 ? "BULLISH" : score <= 40 ? "BEARISH" : "NEUTRAL";

		boolean triggered = score > 70 || score < 30;

		String summary = String.format(
			"Fear & Greed: %d, Exchange Flow: %s BTC, Composite Score: %d",
			latest.fearGreedIndex(),
			latest.btcExchangeFlow() != null ? latest.btcExchangeFlow().toPlainString() : "N/A",
			score
		);

		return new OnChainSignalRecord(
			LocalDateTime.now(),
			latest.fearGreedIndex(),
			latest.fearGreedLabel(),
			score,
			direction,
			triggered,
			"RULE_BASED",
			summary
		);
	}

	private int calculateCompositeScore(Integer fearGreed, BigDecimal exchangeFlow) {
		int score = 50;  // 中性基準

		if (fearGreed < 25)
			score += 20;
		else if (fearGreed < 40)
			score += 10;
		else if (fearGreed > 75)
			score -= 20;
		else if (fearGreed > 60)
			score -= 10;

		if (exchangeFlow != null) {
			double flow = exchangeFlow.doubleValue();
			if (flow < -5000)
				score += 15;
			else if (flow < 0)
				score += 5;
			else if (flow > 5000)
				score -= 15;
			else
				score -= 5;
		}

		return Math.clamp(score, 0, 100);  // 強制 clamp 在 [0, 100]
	}
}

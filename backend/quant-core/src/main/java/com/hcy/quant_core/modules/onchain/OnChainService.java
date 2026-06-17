package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.util.CacheKeyConstants;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.shared.util.RedisPriceReader;
import com.hcy.quant_core.modules.onchain.config.OnChainProperties;
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
	private final RedisPriceReader redisPriceReader;
	private final OnChainProperties properties;

	public OnChainService(OnChainMetricsPersistencePort persistencePort,
		RedisPriceReader redisPriceReader,
		OnChainProperties properties) {
		this.persistencePort = persistencePort;
		this.redisPriceReader = redisPriceReader;
		this.properties = properties;
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

		LOGGER.info("LatestOne OnChainMetrics recordedAt:{}", latest.recordedAt());

		OnChainProperties.CompositeScore cfg = properties.compositeScore();

		int score = calculateCompositeScore(latest.fearGreedIndex(), latest.btcExchangeFlow(),
			cfg);

		String direction = score >= cfg.triggeredBullishThreshold() ? "BULLISH" :
			score <= cfg.triggeredBearishThreshold() ? "BEARISH" : "NEUTRAL";

		boolean triggered =
			score >= cfg.triggeredBullishThreshold() || score <= cfg.triggeredBearishThreshold();

		String summary = String.format(
			"Fear & Greed: %d, Exchange Flow: %s BTC, Composite Score: %d",
			latest.fearGreedIndex(),
			latest.btcExchangeFlow() != null ? latest.btcExchangeFlow().toPlainString() : "N/A",
			score
		);

		BigDecimal btcPrice =
			redisPriceReader.readPriceFromRedis(CacheKeyConstants.latestPrice("BTCUSDT"));

		return new OnChainSignalRecord(
			LocalDateTime.now(),
			latest.fearGreedIndex(),
			latest.fearGreedLabel(),
			score,
			direction,
			triggered,
			"RULE_BASED",
			btcPrice,
			summary
		);
	}

	private int calculateCompositeScore(Integer fearGreed, BigDecimal exchangeFlow,
		OnChainProperties.CompositeScore cfg) {
		int score = 50;  // 中性基準

		if (fearGreed < cfg.extremeFearThreshold())
			score += cfg.extremeFearDelta();
		else if (fearGreed < cfg.fearThreshold())
			score += cfg.fearDelta();
		else if (fearGreed > cfg.extremeGreedThreshold())
			score += cfg.extremeGreedDelta();
		else if (fearGreed > cfg.greedThreshold())
			score += cfg.greedDelta();

		if (exchangeFlow != null) {
			double flow = exchangeFlow.doubleValue();
			if (flow < cfg.highOutflowThreshold())
				score += cfg.highOutflowDelta();
			else if (flow < 0)
				score += cfg.lowOutflowDelta();
			else if (flow > cfg.highInflowThreshold())
				score += cfg.highInflowDelta();
			else
				score += cfg.lowInflowDelta();
		}

		return Math.clamp(score, 0, 100);  // 強制 clamp 在 [0, 100]
	}

}

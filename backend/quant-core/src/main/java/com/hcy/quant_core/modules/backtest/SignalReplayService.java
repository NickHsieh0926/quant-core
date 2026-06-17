package com.hcy.quant_core.modules.backtest;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.model.StrategyPairKey;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;
import com.hcy.quant_core.modules.backtest.model.SignalTradeRecord;
import com.hcy.quant_core.modules.backtest.port.ISignalReplayUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SignalReplayService implements ISignalReplayUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(SignalReplayService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final SignalAlertPersistencePort signalAlertPort;

	public SignalReplayService(SignalAlertPersistencePort signalAlertPort) {
		this.signalAlertPort = signalAlertPort;
	}

	@Override
	public Map<String, PerformanceRecord> replay(String strategy) {
		List<StrategyPairKey> pairs = signalAlertPort.findDistinctStrategyPairs();

		if (strategy != null) {
			pairs = pairs.stream()
				.filter(k -> strategy.equals(k.strategy()))
				.toList();
		}

		Map<String, PerformanceRecord> result = new LinkedHashMap<>();
		for (StrategyPairKey key : pairs) {
			result.put(key.toMapKey(), replaySingle(key));
		}
		return result;
	}

	// single_alert backtest performanceRecord
	private PerformanceRecord replaySingle(StrategyPairKey key) {
		List<SignalAlertRecord> alerts =
			signalAlertPort.findByStrategyAndPairOrderBySignalAt(
				key.strategy(), key.symbolA(), key.symbolB());

		List<SignalTradeRecord> completedTrades = new ArrayList<>();
		SignalAlertRecord activeEntry = null;

		for (SignalAlertRecord alert : alerts) {
			if ("ENTRY".equals(alert.alertType())) {
				if (activeEntry == null) {          // 無持倉 → 開倉
					activeEntry = alert;
				}
				// 有持倉 → 跳過（單一持倉規則）
			} else if ("EXIT".equals(alert.alertType())) {
				if (activeEntry != null) {           // 有持倉 → 平倉
					completedTrades.add(buildTrade(activeEntry, alert));
					activeEntry = null;
				}
				// 無持倉 → 跳過（EXIT 不代表反向開倉）
			}
		}

		return calculatePerformance(key.strategy(), key.symbolA(), key.symbolB(), completedTrades);
	}

	private SignalTradeRecord buildTrade(SignalAlertRecord entry, SignalAlertRecord exit) {
		BigDecimal pnl = calcPnl(
			entry.direction(),
			entry.symbolAPrice(), entry.symbolBPrice(),
			exit.symbolAPrice(), exit.symbolBPrice()
		);

		TRACE.message(
			"entry.strategy:{}, entry.direction:{}, entry.signalAt:{}, entry.symbolAPrice:{}, " +
				"entry.symbolBPrice:{}, exit.signalAt:{}, exit.symbolAPrice:{}," +
				"exit.symbolBPrice:{}, pnl:{}",
			entry.strategy(), entry.direction(), entry.signalAt(), entry.symbolAPrice(),
			entry.symbolBPrice(), exit.signalAt(), exit.symbolAPrice(), exit.symbolBPrice(), pnl);
		
		return new SignalTradeRecord(
			entry.strategy(),
			entry.direction(),
			entry.signalAt(),
			entry.symbolAPrice(),
			entry.symbolBPrice(),
			exit.signalAt(),
			exit.symbolAPrice(),
			exit.symbolBPrice(),
			pnl
		);
	}

	// 單邊 PnL 計算, Profit and Loss（損益）
	private BigDecimal calcPnl(String direction,
		BigDecimal entryA, BigDecimal entryB,
		BigDecimal exitA, BigDecimal exitB) {
		MathContext mc = new MathContext(8, RoundingMode.HALF_UP);
		return switch (direction) {
			case "OPEN_LONG_B" -> pct(entryB, exitB, mc);        // 買 SymbolB
			case "OPEN_SHORT_B" -> pct(exitB, entryB, mc);       // 空 SymbolB
			case "BULLISH" -> pct(entryA, exitA, mc);            // 買 BTC
			case "BEARISH" -> pct(exitA, entryA, mc);            // 空 BTC
			default -> BigDecimal.ZERO;
		};
	}

	// 報酬率換算 return = (after / before - 1) × 100
	private BigDecimal pct(BigDecimal before, BigDecimal after, MathContext mc) {
		if (before == null || before.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		return after.divide(before, mc)
			.subtract(BigDecimal.ONE)
			.multiply(BigDecimal.valueOf(100));
	}

	// 績效計算
	private PerformanceRecord calculatePerformance(String strategy, String symbolA, String symbolB,
		List<SignalTradeRecord> trades) {
		if (trades.isEmpty()) {
			return new PerformanceRecord(
				null,
				strategy,
				symbolA,
				symbolB,
				null,
				null,
				0.0,
				0.0,
				0.0,
				0.0,
				0
			);
		}

		// 損益 List
		List<Double> returns = trades.stream()
			.map(t -> t.pnl().doubleValue())
			.toList();

		double winRate = (double) returns.stream().filter(r -> r > 0).count() / returns.size();
		double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double stdReturn = stdDev(returns, meanReturn);
		double sharpe = stdReturn == 0 ? 0 : meanReturn / stdReturn * Math.sqrt(252);
		double maxDD = calcMaxDrawdown(returns);

		LocalDate startDate = trades.getFirst().entryAt().toLocalDate();
		LocalDate endDate = trades.getLast().exitAt().toLocalDate();

		return new PerformanceRecord(
			null,
			strategy,
			symbolA,
			symbolB,
			startDate,
			endDate,
			sharpe,
			maxDD,
			winRate,
			meanReturn,
			trades.size()
		);
	}

	private double stdDev(List<Double> values, double mean) {
		double variance = values.stream()
			.mapToDouble(v -> Math.pow(v - mean, 2))
			.average().orElse(0);
		return Math.sqrt(variance);
	}

	//equity 本金, peak 歷史本金最高點, maxDD 最大回撤
	private double calcMaxDrawdown(List<Double> returns) {
		double peak = 100.0, equity = 100.0, maxDD = 0.0;
		for (double r : returns) {
			equity *= (1 + r / 100.0);
			if (equity > peak)
				peak = equity;
			double dd = (peak - equity) / peak;
			if (dd > maxDD)
				maxDD = dd;
		}
		return maxDD;
	}
}

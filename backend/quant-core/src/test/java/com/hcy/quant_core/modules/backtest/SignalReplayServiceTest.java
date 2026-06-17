package com.hcy.quant_core.modules.backtest;

import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.model.StrategyPairKey;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalReplayServiceTest {

	@Mock
	private SignalAlertPersistencePort signalAlertPort;

	private SignalReplayService service;

	@BeforeEach
	void setUp() {
		service = new SignalReplayService(signalAlertPort);
	}

	// ── Helper factories ──────────────────────────────────────────────
	private StrategyPairKey statArbPair() {
		return new StrategyPairKey("STAT_ARB", "BTCUSDT", "ETHUSDT");
	}

	private StrategyPairKey onChainPair() {
		return new StrategyPairKey("ON_CHAIN", "BTCUSDT", null);
	}

	private SignalAlertRecord entry(String strategy, String direction,
		BigDecimal priceA, BigDecimal priceB,
		LocalDateTime at) {
		return new SignalAlertRecord(
			strategy, direction, "ENTRY",
			"BTCUSDT", "ETHUSDT",
			priceA, priceB, at
		);
	}

	private SignalAlertRecord exit(String strategy, String direction,
		BigDecimal priceA, BigDecimal priceB,
		LocalDateTime at) {
		return new SignalAlertRecord(
			strategy, direction, "EXIT",
			"BTCUSDT", "ETHUSDT",
			priceA, priceB, at
		);
	}

	// signal_alert 表無任何記錄 → 空 Map，不拋錯
	@Test
	void whenNoPairs_replayReturnsEmptyMap() {
		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of());

		Map<String, PerformanceRecord> result = service.replay(null);

		assertThat(result).isEmpty();
	}

	// 有 pair 但只有 ENTRY 沒有 EXIT → 0 筆完整交易，績效指標全為 0
	@Test
	void whenOnlyEntryNoExit_zeroTrades() {
		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(statArbPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of(
				entry("STAT_ARB", "OPEN_LONG_B",
					new BigDecimal("40000"), new BigDecimal("2800"),
					LocalDateTime.now())
			));

		PerformanceRecord perf = service.replay(null).get("STAT_ARB:BTCUSDT/ETHUSDT");

		assertThat(perf.totalTrades()).isZero();
		assertThat(perf.sharpeRatio()).isZero();
		assertThat(perf.winRate()).isZero();
	}

	// OPEN_LONG_B（買 ETH）：exitB > entryB → PnL 正數
	// entryB=3000, exitB=3300 → PnL = +10%
	@Test
	void openLongB_whenExitPriceHigher_pnlIsPositive() {
		LocalDateTime t1 = LocalDateTime.now();
		LocalDateTime t2 = t1.plusHours(2);

		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(statArbPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of(
				entry("STAT_ARB", "OPEN_LONG_B",
					new BigDecimal("40000"), new BigDecimal("3000"), t1),
				exit("STAT_ARB", "OPEN_LONG_B",
					new BigDecimal("42000"), new BigDecimal("3300"), t2)
			));

		PerformanceRecord perf = service.replay(null).get("STAT_ARB:BTCUSDT/ETHUSDT");

		assertThat(perf.totalTrades()).isEqualTo(1);
		assertThat(perf.annualReturn()).isGreaterThan(0); // PnL = +10%
		assertThat(perf.winRate()).isEqualTo(1.0);
	}

	// OPEN_SHORT_B（空 ETH）：exitB < entryB → PnL 正數
	// entryB=3000, exitB=2700 → PnL = +11.11%
	@Test
	void openShortB_whenExitPriceLower_pnlIsPositive() {
		LocalDateTime t1 = LocalDateTime.now();
		LocalDateTime t2 = t1.plusHours(2);

		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(statArbPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of(
				entry("STAT_ARB", "OPEN_SHORT_B",
					new BigDecimal("40000"), new BigDecimal("3000"), t1),
				exit("STAT_ARB", "OPEN_SHORT_B",
					new BigDecimal("38000"), new BigDecimal("2700"), t2)
			));

		PerformanceRecord perf = service.replay(null).get("STAT_ARB:BTCUSDT/ETHUSDT");

		assertThat(perf.totalTrades()).isEqualTo(1);
		assertThat(perf.annualReturn()).isGreaterThan(0); // PnL = +11.11%
	}

	// BULLISH（買 BTC）：exitA > entryA → PnL 正數
	// entryA=40000, exitA=44000 → PnL = +10%
	@Test
	void bullish_whenExitPriceHigher_pnlIsPositive() {
		LocalDateTime t1 = LocalDateTime.now();
		LocalDateTime t2 = t1.plusDays(7);

		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(onChainPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("ON_CHAIN", "BTCUSDT", null))
			.thenReturn(List.of(
				new SignalAlertRecord("ON_CHAIN", "BULLISH", "ENTRY",
					"BTCUSDT", null, new BigDecimal("40000"), null, t1),
				new SignalAlertRecord("ON_CHAIN", "BULLISH", "EXIT",
					"BTCUSDT", null, new BigDecimal("44000"), null, t2)
			));

		PerformanceRecord perf = service.replay(null).get("ON_CHAIN:BTCUSDT");

		assertThat(perf.totalTrades()).isEqualTo(1);
		assertThat(perf.annualReturn()).isGreaterThan(0); // PnL = +10%
	}

	// 連續兩個 ENTRY → 只認第一個，第二個被跳過（單一持倉規則）
	@Test
	void consecutiveEntries_onlyFirstOneOpensPosition() {
		LocalDateTime t1 = LocalDateTime.now();
		LocalDateTime t2 = t1.plusHours(1);
		LocalDateTime t3 = t1.plusHours(2);

		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(statArbPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of(
				entry("STAT_ARB", "OPEN_LONG_B",
					new BigDecimal("40000"), new BigDecimal("3000"), t1),
				entry("STAT_ARB", "OPEN_LONG_B",      // 第 2 個 ENTRY 被跳過
					new BigDecimal("41000"), new BigDecimal("3100"), t2),
				exit("STAT_ARB", "OPEN_LONG_B",
					new BigDecimal("44000"), new BigDecimal("3300"), t3)
			));

		PerformanceRecord perf = service.replay(null).get("STAT_ARB:BTCUSDT/ETHUSDT");

		assertThat(perf.totalTrades()).isEqualTo(1); // 不是 2
	}

	// EXIT 出現在無持倉時 → 忽略，不拋錯
	@Test
	void exitWithoutEntry_isIgnoredGracefully() {
		LocalDateTime t1 = LocalDateTime.now();
		LocalDateTime t2 = t1.plusHours(1);

		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(statArbPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of(
				exit("STAT_ARB", "OPEN_LONG_B",        // 無持倉 → 跳過
					new BigDecimal("44000"), new BigDecimal("3300"), t1),
				entry("STAT_ARB", "OPEN_LONG_B",       // ENTRY 後無 EXIT
					new BigDecimal("40000"), new BigDecimal("3000"), t2)
			));

		PerformanceRecord perf = service.replay(null).get("STAT_ARB:BTCUSDT/ETHUSDT");

		assertThat(perf.totalTrades()).isZero();
	}

	// strategy = "STAT_ARB" → Map 只含 STAT_ARB，不含 ON_CHAIN
	@Test
	void replayWithStrategyFilter_onlyReturnsMatchingStrategy() {
		when(signalAlertPort.findDistinctStrategyPairs())
			.thenReturn(List.of(statArbPair(), onChainPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of());

		Map<String, PerformanceRecord> result = service.replay("STAT_ARB");

		assertThat(result).containsKey("STAT_ARB:BTCUSDT/ETHUSDT");
		assertThat(result).doesNotContainKey("ON_CHAIN:BTCUSDT");
	}

	// strategy = null → 兩個 strategy 都在回傳 Map 裡
	@Test
	void replayWithNullStrategy_returnsAllStrategies() {
		when(signalAlertPort.findDistinctStrategyPairs())
			.thenReturn(List.of(statArbPair(), onChainPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("STAT_ARB", "BTCUSDT",
			"ETHUSDT"))
			.thenReturn(List.of());
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("ON_CHAIN", "BTCUSDT", null))
			.thenReturn(List.of());

		Map<String, PerformanceRecord> result = service.replay(null);

		assertThat(result).containsKeys("STAT_ARB:BTCUSDT/ETHUSDT", "ON_CHAIN:BTCUSDT");
	}

	// 雙邊策略 key 格式：STRATEGY:symbolA/symbolB
	@Test
	void mapKey_forPairStrategy_includesSlash() {
		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(statArbPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt(any(), any(), any()))
			.thenReturn(List.of());

		Map<String, PerformanceRecord> result = service.replay(null);

		assertThat(result).containsKey("STAT_ARB:BTCUSDT/ETHUSDT");
	}

	// 單邊策略 key 格式：STRATEGY:symbolA（無 symbolB，無斜線）
	@Test
	void mapKey_forSingleLegStrategy_noSlash() {
		when(signalAlertPort.findDistinctStrategyPairs()).thenReturn(List.of(onChainPair()));
		when(signalAlertPort.findByStrategyAndPairOrderBySignalAt("ON_CHAIN", "BTCUSDT", null))
			.thenReturn(List.of());

		Map<String, PerformanceRecord> result = service.replay(null);

		assertThat(result).containsKey("ON_CHAIN:BTCUSDT");
	}
}

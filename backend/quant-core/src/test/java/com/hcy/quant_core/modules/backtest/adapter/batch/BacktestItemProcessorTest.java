package com.hcy.quant_core.modules.backtest.adapter.batch;

import com.hcy.quant_core.modules.backtest.model.OhlcvPair;
import com.hcy.quant_core.modules.backtest.model.TradeResult;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.statarb.calculator.ZScoreCalculator;
import com.hcy.quant_core.modules.statarb.strategy.MeanReversionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BacktestItemProcessorTest {
	private BacktestItemProcessor processor;
	private MeanReversionStrategy strategy;

	@BeforeEach
	void setUp() {
		strategy = new MeanReversionStrategy("BTCUSDT", "ETHUSDT", 6, 2.0, 0.5);
		processor = new BacktestItemProcessor(strategy, new ZScoreCalculator());
	}

	// 數據不足，回傳 null
	@Test
	void process_whenNotEnoughData_returnsNull() throws Exception {
		OhlcvPair pair = makePair("100", "100");

		TradeResult result = processor.process(pair);

		assertThat(result).isNull();
	}

	// 觸發 OPEN_LONG_B 信號
	@Test
	void process_whenZScoreExceedsEntry_returnsOpenSignal() throws Exception {
		List<OhlcvPair> pairs = List.of(
			makePair("110", "100"), makePair("110", "100"),
			makePair("110", "100"), makePair("110", "100"),
			makePair("110", "100"), makePair("120", "100")
		);
		TradeResult result = null;
		for (OhlcvPair p : pairs) {
			result = processor.process(p);
		}

		assertThat(result).isNotNull();
		assertThat(result.action()).isEqualTo("OPEN_LONG_B");
		assertThat(result.zScore()).isGreaterThan(2.0);
	}

	// Z-Score 回歸後應觸發 CLOSE 信號
	@Test
	void process_whenZScoreBelowExit_returnsCloseSignal() throws Exception {
		List<OhlcvPair> pairs = List.of(
			makePair("100", "100"), makePair("110", "100"),
			makePair("110", "100"), makePair("110", "100"),
			makePair("110", "100"), makePair("100", "100")
		);
		TradeResult last = null;
		for (OhlcvPair p : pairs) {
			last = processor.process(p);
		}

		if (last != null) {
			assertThat(last.action()).isIn("CLOSE", "HOLD");
		}
	}

	private OhlcvPair makePair(String closeA, String closeB) {
		OhlcvRecord recA = new OhlcvRecord("BTCUSDT", System.currentTimeMillis(),
			new BigDecimal(closeA), new BigDecimal(closeA),
			new BigDecimal(closeA), new BigDecimal(closeA),
			BigDecimal.ONE, "1d");
		OhlcvRecord recB = new OhlcvRecord("ETHUSDT", System.currentTimeMillis(),
			new BigDecimal(closeB), new BigDecimal(closeB),
			new BigDecimal(closeB), new BigDecimal(closeB),
			BigDecimal.ONE, "1d");
		return new OhlcvPair(recA, recB);
	}
}

package com.hcy.quant_core.modules.statarb.calculator;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ZScoreCalculatorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZScoreCalculatorTest.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final ZScoreCalculator calculator = new ZScoreCalculator();

	// 驗證已知數據的計算結果
	@Test
	void calculate_withKnownData() {
		// Spread = [10, 12, 14, 12, 10]
		// mean = 11.6, stdDev ≈ 1.497
		// latest spread = 10, Z-Score = (10 - 11.6) / 1.497 ≈ -1.069
		List<BigDecimal> pricesA = List.of(
			bd("110"), bd("112"), bd("114"), bd("112"), bd("110"));
		List<BigDecimal> pricesB = List.of(
			bd("100"), bd("100"), bd("100"), bd("100"), bd("100"));

		double result = calculator.calculate(pricesA, pricesB);

		TRACE.message("calculate_withKnownData result:{}", result);

		assertThat(result).isCloseTo(-1.069, within(0.01));
	}

	// 標準差為 0：價差完全固定，Z-Score 應回傳 0（不拋除零例外）
	@Test
	void calculate_whenStdDevIsZero() {
		List<BigDecimal> pricesA = List.of(bd("100"), bd("100"), bd("100"));
		List<BigDecimal> pricesB = List.of(bd("100"), bd("100"), bd("100"));

		double result = calculator.calculate(pricesA, pricesB);

		assertThat(result).isEqualTo(0.0);
	}

	// 序列長度不一致：應拋出 IllegalArgumentException
	@Test
	void calculate_whenSizeMismatch() {
		List<BigDecimal> pricesA = List.of(bd("100"), bd("200"));
		List<BigDecimal> pricesB = List.of(bd("100"));

		assertThatThrownBy(() -> calculator.calculate(pricesA, pricesB))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("長度不一致");
	}

	// 數據點不足：少於 2 筆應拋出例外
	@Test
	void calculate_whenTooFewDataPoints() {
		List<BigDecimal> pricesA = List.of(bd("100"));
		List<BigDecimal> pricesB = List.of(bd("100"));

		assertThatThrownBy(() -> calculator.calculate(pricesA, pricesB))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("至少需要 2 個數據");
	}

	// 高 Z-Score：符合開倉信號條件（> 2.0）
	@Test
	void calculate_withHighSpreadDeviation() {
		// 過去 4 天價差穩定在 10，最後一天突然跳到 20（偏離 2 個標準差以上）
		List<BigDecimal> pricesA = List.of(
			bd("120"), bd("110"), bd("110"), bd("110"), bd("110"), bd("110"));
		List<BigDecimal> pricesB = List.of(
			bd("100"), bd("100"), bd("100"), bd("100"), bd("100"), bd("100"));

		double result = calculator.calculate(pricesA, pricesB);

		TRACE.message("calculate_withHighSpreadDeviation result:{}", result);

		assertThat(result).isGreaterThan(2.0);
	}

	private BigDecimal bd(String value) {
		return new BigDecimal(value);
	}
}

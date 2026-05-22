package com.hcy.quant_core.modules.statarb.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ZScoreCalculator {

	public double calculate(List<BigDecimal> pricesA, List<BigDecimal> pricesB) {
		if (pricesA.size() != pricesB.size()) {
			throw new IllegalArgumentException(
				"價格序列長度不一致: A=" + pricesA.size() + ", B=" + pricesB.size());
		}
		if (pricesA.size() < 2) {
			throw new IllegalArgumentException("計算 Z-Score 至少需要 2 個數據");
		}

		List<Double> spreads = new ArrayList<>();
		for (int i = 0; i < pricesA.size(); i++) {
			double spread = pricesA.get(i).doubleValue() - pricesB.get(i).doubleValue();
			spreads.add(spread);
		}

		// 歷史均值
		double mean = spreads.stream().mapToDouble(d -> d).average().orElse(0);

		// 母體標準差（Population StdDev）
		double variance = spreads.stream()
			.mapToDouble(d -> Math.pow(d - mean, 2))
			.average().orElse(0);
		double stdDev = Math.sqrt(variance);

		// 標準差為 0 代表兩個資產價差完全固定，Z-Score 無意義。
		if (stdDev == 0)
			return 0;

		double latestSpread = spreads.get(spreads.size() - 1);
		return (latestSpread - mean) / stdDev;
	}
}

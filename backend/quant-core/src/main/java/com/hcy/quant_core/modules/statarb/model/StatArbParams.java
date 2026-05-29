package com.hcy.quant_core.modules.statarb.model;

public record StatArbParams(
	double entryThreshold,
	double exitThreshold,
	int lookbackSize
) {
	// 即時交易使用的預設值
	public static StatArbParams defaults() {
		return new StatArbParams(2.0, 0.5, 30);
	}
}

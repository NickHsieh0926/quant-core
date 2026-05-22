package com.hcy.quant_core.modules.statarb.strategy;

public record MeanReversionStrategy(
	String symbolA,
	String symbolB,
	int lookBackDays,    // 滾動視窗
	double entryZScore,
	double exitZScore
) implements Strategy {}

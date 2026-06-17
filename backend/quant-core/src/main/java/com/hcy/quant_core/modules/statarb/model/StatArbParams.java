package com.hcy.quant_core.modules.statarb.model;

public record StatArbParams(
	double entryThreshold,
	double exitThreshold,
	double stopLossThreshold,
	int lookbackSize
) {}

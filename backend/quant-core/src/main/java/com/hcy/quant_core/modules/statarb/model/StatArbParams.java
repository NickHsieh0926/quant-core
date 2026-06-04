package com.hcy.quant_core.modules.statarb.model;

public record StatArbParams(
	double entryThreshold,
	double exitThreshold,
	int lookbackSize
) {}

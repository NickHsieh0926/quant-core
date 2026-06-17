package com.hcy.quant_core.modules.strategy;

public record MaCrossStrategy(
	String symbol,
	int shortPeriod,
	int longPeriod
) implements Strategy {}

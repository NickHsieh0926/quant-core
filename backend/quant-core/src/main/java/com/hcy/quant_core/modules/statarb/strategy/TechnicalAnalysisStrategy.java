package com.hcy.quant_core.modules.statarb.strategy;

public record TechnicalAnalysisStrategy(
	String symbol,
	String indicatorType
) implements Strategy {}

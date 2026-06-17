package com.hcy.quant_core.modules.strategy;

public record TechnicalAnalysisStrategy(
	String symbol,
	String indicatorType
) implements Strategy {}

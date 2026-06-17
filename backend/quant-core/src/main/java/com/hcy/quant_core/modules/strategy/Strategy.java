package com.hcy.quant_core.modules.strategy;

public sealed interface Strategy
	permits MeanReversionStrategy, OnChainSignalStrategy,
	MaCrossStrategy, TechnicalAnalysisStrategy {}

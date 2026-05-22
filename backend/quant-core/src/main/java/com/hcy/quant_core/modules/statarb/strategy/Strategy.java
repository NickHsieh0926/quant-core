package com.hcy.quant_core.modules.statarb.strategy;

public sealed interface Strategy
	permits MeanReversionStrategy, OnChainSignalStrategy,
	MaCrossStrategy, TechnicalAnalysisStrategy {}

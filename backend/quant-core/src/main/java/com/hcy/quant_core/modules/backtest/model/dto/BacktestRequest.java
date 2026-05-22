package com.hcy.quant_core.modules.backtest.model.dto;

public record BacktestRequest(
	String symbolA,
	String symbolB,
	int lookBackDays,
	double entryZScore,
	double exitZScore,
	String interval
) {}

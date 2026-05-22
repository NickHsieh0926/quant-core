package com.hcy.quant_core.modules.backtest.model;

import java.time.LocalDate;

public record PerformanceRecord(
	String jobId,
	String strategy,
	String symbolA,
	String symbolB,
	LocalDate startDate,
	LocalDate endDate,
	double sharpeRatio,
	double maxDrawdown,
	double winRate,
	double annualReturn,
	int totalTrades
) {}

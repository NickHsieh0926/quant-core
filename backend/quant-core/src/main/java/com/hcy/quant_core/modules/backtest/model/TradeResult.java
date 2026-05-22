package com.hcy.quant_core.modules.backtest.model;

import java.math.BigDecimal;

public record TradeResult(
	long openTime,
	String action,
	double zScore,
	BigDecimal priceA,
	BigDecimal priceB
) {}

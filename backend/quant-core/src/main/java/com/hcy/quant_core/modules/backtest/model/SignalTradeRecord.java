package com.hcy.quant_core.modules.backtest.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignalTradeRecord(
	String strategy,
	String direction,
	LocalDateTime entryAt,
	BigDecimal entryPriceA,
	BigDecimal entryPriceB,
	LocalDateTime exitAt,
	BigDecimal exitPriceA,
	BigDecimal exitPriceB,
	BigDecimal pnl
) {}

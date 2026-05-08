package com.hcy.quant_core.modules.marketdata.model;

import java.math.BigDecimal;

public record OhlcvRecord(
	String symbol,
	long openTime,
	BigDecimal open,
	BigDecimal high,
	BigDecimal low,
	BigDecimal close,
	BigDecimal volume,
	String interval
) {}

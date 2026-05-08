package com.hcy.quant_core.modules.marketdata.model;

public record IngestionRequest(
	String symbol,
	String interval
) {}

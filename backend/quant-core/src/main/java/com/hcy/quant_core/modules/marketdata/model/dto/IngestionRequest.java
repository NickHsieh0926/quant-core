package com.hcy.quant_core.modules.marketdata.model.dto;

public record IngestionRequest(
	String symbol,
	String interval
) {}

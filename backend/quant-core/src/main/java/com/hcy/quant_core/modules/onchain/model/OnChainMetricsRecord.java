package com.hcy.quant_core.modules.onchain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OnChainMetricsRecord(
	LocalDateTime recordedAt,
	Integer fearGreedIndex,
	String fearGreedLabel,
	BigDecimal btcExchangeFlow,
	BigDecimal nupl,
	BigDecimal sopr
) {}

package com.hcy.quant_core.modules.onchain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OnChainSignalRecord(
	LocalDateTime signalAt,
	int fearGreedIndex,
	String fearGreedLabel,
	int compositeScore,
	String direction,
	boolean triggered,
	String source,
	BigDecimal symbolAPrice,
	String summary
) {}

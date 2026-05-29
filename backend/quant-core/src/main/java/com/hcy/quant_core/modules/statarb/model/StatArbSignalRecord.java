package com.hcy.quant_core.modules.statarb.model;

import java.time.LocalDateTime;

public record StatArbSignalRecord(
	String symbolA,
	String symbolB,
	double zScore,
	String direction,
	boolean triggered,
	LocalDateTime signalAt
) {}

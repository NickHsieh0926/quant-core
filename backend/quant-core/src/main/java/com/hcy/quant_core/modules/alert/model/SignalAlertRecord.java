package com.hcy.quant_core.modules.alert.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignalAlertRecord(
	String strategy,
	String direction,
	String alertType,
	String symbolA,
	String symbolB,
	BigDecimal symbolAPrice,
	BigDecimal symbolBPrice,
	LocalDateTime signalAt
) {}

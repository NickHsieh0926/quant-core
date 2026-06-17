package com.hcy.quant_core.modules.websocket.model;

import com.hcy.quant_core.modules.websocket.model.direction.SignalDirection;

import java.time.LocalDateTime;

public record SignalAlertPayload(
	String type,
	String symbolPair,
	SignalDirection direction,
	double score,
	AlertType alertType,
	String summary,
	LocalDateTime signalAt
) {}

package com.hcy.quant_core.modules.websocket.model.direction;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = SignalDirectionSerializer.class)
public sealed interface SignalDirection
	permits StatArbDirection, OnChainDirection {}

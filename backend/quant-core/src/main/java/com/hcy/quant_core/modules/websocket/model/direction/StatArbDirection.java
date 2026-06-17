package com.hcy.quant_core.modules.websocket.model.direction;

public enum StatArbDirection implements SignalDirection {
	// 統計套利信號的操作方向
	OPEN_LONG_B,
	OPEN_SHORT_B,
	CLOSE,
	HOLD,
	STOP_LOSS
}

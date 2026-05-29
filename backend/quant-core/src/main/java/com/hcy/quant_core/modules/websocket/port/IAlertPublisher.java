package com.hcy.quant_core.modules.websocket.port;

import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;

public interface IAlertPublisher {

	// 統一推播入口
	void publish(SignalAlertPayload payload);

}

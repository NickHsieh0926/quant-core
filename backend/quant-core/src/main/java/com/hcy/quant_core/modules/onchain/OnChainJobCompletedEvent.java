package com.hcy.quant_core.modules.onchain;

import org.springframework.context.ApplicationEvent;

public class OnChainJobCompletedEvent extends ApplicationEvent {
	public OnChainJobCompletedEvent(Object source) {
		super(source);
	}
}

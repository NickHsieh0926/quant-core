package com.hcy.quant_core.modules.websocket.model;

public enum AlertTopicType {
	STAT_ARB("/topic/statarb"),
	ON_CHAIN("/topic/onchain"),
	ALERT("/topic/alert");

	private final String path;

	AlertTopicType(String path) {
		this.path = path;
	}

	public String path() {
		return path;
	}
}

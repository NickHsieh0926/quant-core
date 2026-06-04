package com.hcy.quant_core.modules.onchain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.onchain")
public record OnChainProperties(
	FearGreed fearGreed
) {
	public record FearGreed(
		int bullishThreshold,
		int bearishThreshold
	) {}
}

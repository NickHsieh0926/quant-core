package com.hcy.quant_core.modules.statarb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.statarb")
public record StatArbProperties(
	List<PairConfig> pairs
) {
	public record PairConfig(
		String symbolA,
		String symbolB,
		double zscoreThreshold,
		double zscoreExitThreshold,
		double zscoreStopLossThreshold,
		int lookbackSize
	) {}
}

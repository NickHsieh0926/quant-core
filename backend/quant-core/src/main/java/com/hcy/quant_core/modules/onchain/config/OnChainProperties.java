package com.hcy.quant_core.modules.onchain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.onchain")
public record OnChainProperties(
	CompositeScore compositeScore
) {
	public record CompositeScore(
		// Fear & Greed 閾值
		int extremeFearThreshold,
		int fearThreshold,
		int greedThreshold,
		int extremeGreedThreshold,
		// Fear & Greed 分數權重
		int extremeFearDelta,
		int fearDelta,
		int greedDelta,
		int extremeGreedDelta,
		// 交易所流量閾值
		double highOutflowThreshold,
		double highInflowThreshold,
		// 交易所流量分數權重
		int highOutflowDelta,
		int lowOutflowDelta,
		int lowInflowDelta,
		int highInflowDelta,
		// 信號觸發閾值
		int triggeredBullishThreshold,
		int triggeredBearishThreshold
	) {}
}

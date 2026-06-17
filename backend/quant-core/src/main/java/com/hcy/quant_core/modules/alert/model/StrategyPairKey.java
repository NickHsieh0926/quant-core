package com.hcy.quant_core.modules.alert.model;

public record StrategyPairKey(
	String strategy,
	String symbolA,
	String symbolB
) {
	public String toMapKey() {
		return symbolB != null
			? strategy + ":" + symbolA + "/" + symbolB
			: strategy + ":" + symbolA;
	}
}

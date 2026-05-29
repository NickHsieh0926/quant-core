package com.hcy.quant_core.infrastructure.shared.util;

public class CacheKeyConstants {
	
	private CacheKeyConstants() {
	}

	// 最新即時報價
	public static String latestPrice(String symbol) {
		return "price:" + symbol.toUpperCase();
	}

	// 已收盤 K 線收盤價列表（Z-Score 滑動窗口計算的原始數據來源）
	public static String closedPriceList(String symbol) {
		return "price:closed:" + symbol.toUpperCase();
	}

	// Z-Score最新計算結果
	public static String zScore(String symbolA, String symbolB) {
		return "zscore:" + symbolA.toUpperCase() + ":" + symbolB.toUpperCase();
	}
}

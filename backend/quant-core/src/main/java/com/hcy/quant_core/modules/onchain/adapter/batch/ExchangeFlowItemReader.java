package com.hcy.quant_core.modules.onchain.adapter.batch;

import org.springframework.batch.item.ItemReader;

import java.math.BigDecimal;

public class ExchangeFlowItemReader implements ItemReader<BigDecimal> {

	private boolean fetched = false;

	@Override
	public BigDecimal read() {
		if (fetched)
			return null;
		fetched = true;
		// TODO：串接 CryptoQuant / Glassnode API，取得當日 BTC 交易所淨流量
		// 暫時回傳 mock 值
		return new BigDecimal("-3200");
	}
}

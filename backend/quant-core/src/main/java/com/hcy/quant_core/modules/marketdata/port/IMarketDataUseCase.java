package com.hcy.quant_core.modules.marketdata.port;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;

import java.util.List;

public interface IMarketDataUseCase {
	// OHLCV History
	List<OhlcvRecord> getOhlcv(String symbol, String interval);

	// Trigger Spring Batch Job
	String triggerIngestion(String symbol, String interval);
}

package com.hcy.quant_core.modules.marketdata.port;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;

import java.util.List;

public interface OhlcvPersistencePort {
	void save(OhlcvRecord record);

	List<OhlcvRecord> findBySymbolAndInterval(String symbol, String interval);

	boolean existsBySymbolAndOpenTimeAndInterval(String symbol, long openTime, String interval);

	Long getMaxOpenTime(String symbol, String interval);
}

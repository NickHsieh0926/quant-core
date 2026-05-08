package com.hcy.quant_core.modules.marketdata.adapter.batch;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.springframework.batch.item.ItemProcessor;

public class OhlcvItemProcessor implements ItemProcessor<OhlcvRecord, OhlcvRecord> {
	private final OhlcvPersistencePort persistencePort;

	public OhlcvItemProcessor(OhlcvPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public OhlcvRecord process(OhlcvRecord record) {
		// Data存在則跳過
		if (persistencePort.existsBySymbolAndOpenTimeAndInterval(
			record.symbol(), record.openTime(), record.interval())) {
			return null;
		}
		return record;
	}
}

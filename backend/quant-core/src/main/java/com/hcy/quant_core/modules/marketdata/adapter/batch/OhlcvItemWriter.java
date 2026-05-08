package com.hcy.quant_core.modules.marketdata.adapter.batch;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class OhlcvItemWriter implements ItemWriter<OhlcvRecord> {
	private final OhlcvPersistencePort persistencePort;

	public OhlcvItemWriter(OhlcvPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public void write(Chunk<? extends OhlcvRecord> chunk) {
		chunk.getItems().forEach(persistencePort::save);
	}
}

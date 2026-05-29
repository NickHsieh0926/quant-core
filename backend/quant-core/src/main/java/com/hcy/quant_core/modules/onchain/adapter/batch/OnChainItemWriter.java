package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class OnChainItemWriter implements ItemWriter<OnChainMetricsRecord> {
	private final OnChainMetricsPersistencePort persistencePort;

	public OnChainItemWriter(OnChainMetricsPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public void write(Chunk<? extends OnChainMetricsRecord> chunk) {
		chunk.getItems().forEach(persistencePort::save);
	}
}

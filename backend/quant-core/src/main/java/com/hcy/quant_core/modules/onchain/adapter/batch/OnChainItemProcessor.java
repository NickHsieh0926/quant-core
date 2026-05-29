package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.springframework.batch.item.ItemProcessor;

public class OnChainItemProcessor
	implements ItemProcessor<OnChainMetricsRecord, OnChainMetricsRecord> {
	private final OnChainMetricsPersistencePort persistencePort;

	public OnChainItemProcessor(OnChainMetricsPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public OnChainMetricsRecord process(OnChainMetricsRecord record) {
		if (persistencePort.existsByRecordedAt(record.recordedAt())) {
			return null;
		}
		return record;
	}
}

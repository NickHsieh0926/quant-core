package com.hcy.quant_core.modules.onchain.adapter.persistence;


import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;
import com.hcy.quant_core.modules.onchain.model.entity.OnChainSignalEntity;
import com.hcy.quant_core.modules.onchain.port.OnChainSignalPersistencePort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class OnChainSignalJpaAdapter implements OnChainSignalPersistencePort {

	private final OnChainSignalRepository repository;

	public OnChainSignalJpaAdapter(OnChainSignalRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(OnChainSignalRecord record) {
		OnChainSignalEntity e = new OnChainSignalEntity();
		e.setSignalAt(record.signalAt());
		e.setFearGreedIndex(record.fearGreedIndex());
		e.setFearGreedLabel(record.fearGreedLabel());
		e.setCompositeScore(record.compositeScore());
		e.setDirection(record.direction());
		e.setTriggered(record.triggered());
		e.setSource(record.source());
		e.setSymbolAPrice(record.symbolAPrice());
		e.setSummary(record.summary());
		e.setCreatedAt(OffsetDateTime.now());
		repository.save(e);
	}
}

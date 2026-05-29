package com.hcy.quant_core.modules.statarb.adapter;

import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.model.entity.StatArbSignalEntity;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class StatArbSignalJpaAdapter implements StatArbSignalPersistencePort {
	private final StatArbSignalRepository repository;

	public StatArbSignalJpaAdapter(StatArbSignalRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(StatArbSignalRecord signal) {
		repository.save(toEntity(signal));
	}

	@Override
	public List<StatArbSignalRecord> findLatest(int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit);
		return repository.findAllByOrderBySignalAtDesc(pageRequest)
			.stream()
			.map(this::toRecord)
			.toList();
	}

	private StatArbSignalEntity toEntity(StatArbSignalRecord r) {
		StatArbSignalEntity e = new StatArbSignalEntity();
		e.setSymbolA(r.symbolA());
		e.setSymbolB(r.symbolB());
		e.setZScore(BigDecimal.valueOf(r.zScore()));
		e.setDirection(r.direction());
		e.setTriggered(r.triggered());
		e.setSignalAt(r.signalAt());
		return e;
	}

	private StatArbSignalRecord toRecord(StatArbSignalEntity e) {
		return new StatArbSignalRecord(
			e.getSymbolA(),
			e.getSymbolB(),
			e.getZScore().doubleValue(),
			e.getDirection(),
			e.isTriggered(),
			e.getSignalAt()
		);
	}
}

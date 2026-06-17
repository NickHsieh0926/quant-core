package com.hcy.quant_core.modules.alert.adapter.persistence;

import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.model.StrategyPairKey;
import com.hcy.quant_core.modules.alert.model.entity.SignalAlertEntity;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SignalAlertJpaAdapter implements SignalAlertPersistencePort {

	private final SignalAlertRepository repository;

	public SignalAlertJpaAdapter(SignalAlertRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(SignalAlertRecord record) {
		repository.save(toEntity(record));
	}

	@Override
	public List<StrategyPairKey> findDistinctStrategyPairs() {
		return repository.findDistinctStrategyPairs().stream()
			.map(row -> new StrategyPairKey(
				(String) row[0],
				(String) row[1],
				(String) row[2]))
			.toList();
	}

	@Override
	public List<SignalAlertRecord> findByStrategyAndPairOrderBySignalAt(
		String strategy, String symbolA, String symbolB) {
		List<SignalAlertEntity> entities = (symbolB == null)
			?
			repository.findByStrategyAndSymbolAAndSymbolBIsNullOrderBySignalAtAsc(strategy,
				symbolA)
			:
			repository.findByStrategyAndSymbolAAndSymbolBOrderBySignalAtAsc(strategy, symbolA,
				symbolB);
		return entities.stream().map(this::toRecord).toList();
	}

	@Override
	public List<SignalAlertRecord> findByStrategyOrderBySignalAt(String strategy) {
		return repository.findByStrategyOrderBySignalAtAsc(strategy)
			.stream().map(this::toRecord).toList();
	}

	@Override
	public List<String> findDistinctStrategies() {
		return repository.findDistinctStrategies();
	}

	private SignalAlertEntity toEntity(SignalAlertRecord r) {
		SignalAlertEntity e = new SignalAlertEntity();
		e.setStrategy(r.strategy());
		e.setDirection(r.direction());
		e.setAlertType(r.alertType());
		e.setSymbolA(r.symbolA());
		e.setSymbolB(r.symbolB());
		e.setSymbolAPrice(r.symbolAPrice());
		e.setSymbolBPrice(r.symbolBPrice());
		e.setSignalAt(r.signalAt());
		return e;
	}

	private SignalAlertRecord toRecord(SignalAlertEntity e) {
		return new SignalAlertRecord(
			e.getStrategy(),
			e.getDirection(),
			e.getAlertType(),
			e.getSymbolA(),
			e.getSymbolB(),
			e.getSymbolAPrice(),
			e.getSymbolBPrice(),
			e.getSignalAt()
		);
	}
}

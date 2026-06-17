package com.hcy.quant_core.modules.alert.port;

import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.model.StrategyPairKey;

import java.util.List;

public interface SignalAlertPersistencePort {

	void save(SignalAlertRecord record);

	List<StrategyPairKey> findDistinctStrategyPairs();

	List<SignalAlertRecord> findByStrategyAndPairOrderBySignalAt(
		String strategy, String symbolA, String symbolB);

	List<SignalAlertRecord> findByStrategyOrderBySignalAt(String strategy);

	List<String> findDistinctStrategies();
}

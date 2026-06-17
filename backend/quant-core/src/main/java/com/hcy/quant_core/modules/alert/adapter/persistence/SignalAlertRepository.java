package com.hcy.quant_core.modules.alert.adapter.persistence;

import com.hcy.quant_core.modules.alert.model.entity.SignalAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SignalAlertRepository extends JpaRepository<SignalAlertEntity, Long> {

	List<SignalAlertEntity> findByStrategyOrderBySignalAtAsc(String strategy);

	@Query("SELECT DISTINCT s.strategy FROM SignalAlertEntity s")
	List<String> findDistinctStrategies();

	@Query("SELECT s.strategy, s.symbolA, s.symbolB " +
		"FROM SignalAlertEntity s " +
		"GROUP BY s.strategy, s.symbolA, s.symbolB")
	List<Object[]> findDistinctStrategyPairs();

	// ON_CHAIN, symbolB IS NULL
	List<SignalAlertEntity> findByStrategyAndSymbolAAndSymbolBIsNullOrderBySignalAtAsc(
		String strategy, String symbolA);

	List<SignalAlertEntity> findByStrategyAndSymbolAAndSymbolBOrderBySignalAtAsc(
		String strategy, String symbolA, String symbolB);
}

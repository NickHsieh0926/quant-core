package com.hcy.quant_core.modules.backtest.port;

import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;

import java.util.List;
import java.util.Optional;

public interface BacktestResultPersistencePort {
	void save(PerformanceRecord record);

	Optional<PerformanceRecord> findByJobId(String jobId);

	List<PerformanceRecord> findAll();
}

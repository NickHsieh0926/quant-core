package com.hcy.quant_core.modules.onchain.port;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface OnChainMetricsPersistencePort {
	void save(OnChainMetricsRecord record);

	boolean existsByRecordedAt(LocalDateTime recordedAt);

	List<OnChainMetricsRecord> findLatest(int limit);

	List<OnChainMetricsRecord> findAll();

	OnChainMetricsRecord findLatestOne();
}

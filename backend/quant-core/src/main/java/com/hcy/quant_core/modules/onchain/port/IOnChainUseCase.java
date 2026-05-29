package com.hcy.quant_core.modules.onchain.port;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;

import java.util.List;

public interface IOnChainUseCase {
	List<OnChainMetricsRecord> getLatestMetrics(int limit);

	List<OnChainMetricsRecord> getAllMetrics();

	OnChainSignalRecord calculateSignal();
}

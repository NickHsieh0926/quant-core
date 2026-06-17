package com.hcy.quant_core.modules.backtest.port;

import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;

import java.util.Map;

public interface ISignalReplayUseCase {
	Map<String, PerformanceRecord> replay(String strategy);
}

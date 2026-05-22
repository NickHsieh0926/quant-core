package com.hcy.quant_core.modules.backtest.port;

import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;

import java.util.List;
import java.util.Optional;

public interface IBacktestUseCase {
	String run(String symbolA, String symbolB, int lookBackDays,
		double entryZScore, double exitZScore, String interval);

	Optional<PerformanceRecord> getResult(String jobId);

	List<PerformanceRecord> getHistory();
}

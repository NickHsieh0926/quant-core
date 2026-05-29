package com.hcy.quant_core.modules.statarb.port;

import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;

import java.util.List;

public interface IStatArbUseCase {
	List<StatArbSignalRecord> getRecentSignals(int limit);

	StatArbSignalRecord calculate(String symbolA, String symbolB, StatArbParams params);
}

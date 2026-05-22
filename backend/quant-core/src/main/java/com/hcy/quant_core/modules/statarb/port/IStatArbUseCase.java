package com.hcy.quant_core.modules.statarb.port;

import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;

import java.util.List;

public interface IStatArbUseCase {
	List<StatArbSignalRecord> getRecentSignals(int limit);
}

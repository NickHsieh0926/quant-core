package com.hcy.quant_core.modules.onchain.port;

import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;

public interface OnChainSignalPersistencePort {
	void save(OnChainSignalRecord record);
}

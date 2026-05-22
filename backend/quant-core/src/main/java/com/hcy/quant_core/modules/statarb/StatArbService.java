package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatArbService implements IStatArbUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatArbService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final StatArbSignalPersistencePort persistencePort;

	public StatArbService(StatArbSignalPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public List<StatArbSignalRecord> getRecentSignals(int limit) {
		TRACE.message("getRecentSignals limit:{}", limit);
		return persistencePort.findLatest(limit);
	}
}

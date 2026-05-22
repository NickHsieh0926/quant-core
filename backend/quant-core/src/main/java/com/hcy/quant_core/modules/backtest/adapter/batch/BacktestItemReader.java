package com.hcy.quant_core.modules.backtest.adapter.batch;

import com.hcy.quant_core.modules.backtest.model.OhlcvPair;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.springframework.batch.item.ItemReader;

import java.util.*;

public class BacktestItemReader implements ItemReader<OhlcvPair> {
	private final OhlcvPersistencePort persistencePort;
	private final String symbolA;
	private final String symbolB;
	private final String interval;
	private final Queue<OhlcvPair> buffer = new LinkedList<>();
	private boolean loaded = false;

	public BacktestItemReader(OhlcvPersistencePort persistencePort,
		String symbolA, String symbolB, String interval) {
		this.persistencePort = persistencePort;
		this.symbolA = symbolA;
		this.symbolB = symbolB;
		this.interval = interval;
	}

	@Override
	public OhlcvPair read() {
		if (!loaded) {
			loadAndPair();
			loaded = true;
		}
		return buffer.poll();
	}

	private void loadAndPair() {
		List<OhlcvRecord> listA = persistencePort.findBySymbolAndInterval(symbolA, interval);
		List<OhlcvRecord> listB = persistencePort.findBySymbolAndInterval(symbolB, interval);

		// 以 openTime 為 key，將 B 存成 Map，再逐一配對 A
		Map<Long, OhlcvRecord> mapB = new HashMap<>();
		listB.forEach(r -> mapB.put(r.openTime(), r));

		listA.stream()
			.filter(a -> mapB.containsKey(a.openTime()))
			.sorted(Comparator.comparingLong(OhlcvRecord::openTime))
			.forEach(a -> buffer.add(new OhlcvPair(a, mapB.get(a.openTime()))));
	}
}

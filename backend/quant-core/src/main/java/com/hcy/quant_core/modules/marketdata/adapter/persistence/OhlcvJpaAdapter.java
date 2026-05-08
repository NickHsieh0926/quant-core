package com.hcy.quant_core.modules.marketdata.adapter.persistence;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.model.entity.OhlcvEntity;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OhlcvJpaAdapter implements OhlcvPersistencePort {

	private final OhlcvRepository ohlcvRepository;

	public OhlcvJpaAdapter(OhlcvRepository ohlcvRepository) {
		this.ohlcvRepository = ohlcvRepository;
	}

	@Override
	public void save(OhlcvRecord record) {
		ohlcvRepository.save(toEntity(record));
	}

	@Override
	public List<OhlcvRecord> findBySymbolAndInterval(String symbol, String interval) {
		return ohlcvRepository
			.findBySymbolAndIntervalOrderByOpenTimeAsc(symbol, interval)
			.stream()
			.map(this::toRecord)
			.toList();
	}

	@Override
	public boolean existsBySymbolAndOpenTimeAndInterval(String symbol, long openTime,
		String interval) {
		return ohlcvRepository.existsBySymbolAndOpenTimeAndInterval(symbol, openTime, interval);
	}

	@Override
	public Long getMaxOpenTime(String symbol, String interval) {
		return ohlcvRepository.getMaxOpenTime(symbol, interval);
	}

	private OhlcvEntity toEntity(OhlcvRecord r) {
		OhlcvEntity e = new OhlcvEntity();
		e.setSymbol(r.symbol());
		e.setOpenTime(r.openTime());
		e.setOpen(r.open());
		e.setHigh(r.high());
		e.setLow(r.low());
		e.setClose(r.close());
		e.setVolume(r.volume());
		e.setInterval(r.interval());
		return e;
	}

	private OhlcvRecord toRecord(OhlcvEntity e) {
		return new OhlcvRecord(
			e.getSymbol(),
			e.getOpenTime(),
			e.getOpen(),
			e.getHigh(),
			e.getLow(),
			e.getClose(),
			e.getVolume(),
			e.getInterval()
		);
	}
}

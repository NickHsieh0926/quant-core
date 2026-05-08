package com.hcy.quant_core.modules.marketdata.adapter.batch;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class BinanceItemReader implements ItemReader<OhlcvRecord> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BinanceItemReader.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final RestTemplate restTemplate;
	private final String symbol;
	private final String interval;

	private Queue<OhlcvRecord> buffer = new LinkedList<>();
	private long currentStart;
	private boolean done = false;

	public BinanceItemReader(RestTemplate restTemplate, String symbol,
		String interval, long startTime) {
		this.restTemplate = restTemplate;
		this.symbol = symbol;
		this.interval = interval;
		this.currentStart = startTime;
	}

	@CircuitBreaker(name = "binanceApi", fallbackMethod = "fallbackRead")
	@Retry(name = "binanceApi")
	@Override
	public OhlcvRecord read() {
		TRACE.message("buffer.isEmpty={}, done={}", buffer.isEmpty(), done);
		if (!buffer.isEmpty())
			return buffer.poll();
		if (done)
			return null;

		LOGGER.info("Request Binance API Param symbol={}, interval={}, currentStart={}", symbol,
			interval, currentStart);
		// Binance 回傳 [ [openTime, open, high, low, close, volume, ...], ... ]
		String url = "https://api.binance.com/api/v3/klines"
			+ "?symbol={s}&interval={i}&startTime={t}&limit=1000";

		Object[][] raw = restTemplate.getForObject(url, Object[][].class,
			Map.of("s", symbol, "i", interval, "t", currentStart));

		if (raw == null || raw.length == 0) {
			TRACE.message("Binance API Response raw == null || raw.length == 0");
			done = true;
			return null;
		}

		for (Object[] kline : raw) {
			buffer.add(new OhlcvRecord(
				symbol,
				((Number) kline[0]).longValue(),
				new BigDecimal(kline[1].toString()),
				new BigDecimal(kline[2].toString()),
				new BigDecimal(kline[3].toString()),
				new BigDecimal(kline[4].toString()),
				new BigDecimal(kline[5].toString()),
				interval
			));
		}

		currentStart = ((Number) raw[raw.length - 1][0]).longValue() + 1;
		LOGGER.info("Next Request Binance API currentStart={}", currentStart);

		if (raw.length < 1000) {
			TRACE.message("raw.length < 1000 , Last Batch Of Data");
			done = true;
		}

		return buffer.poll();
	}

	public OhlcvRecord fallbackRead(Throwable e) {
		LOGGER.warn("Binance API unavailable, stopping ingestion: {}", e.getMessage());
		return null;  // Spring Batch：null = 資料結束，Job 以 COMPLETED 結束
	}
}

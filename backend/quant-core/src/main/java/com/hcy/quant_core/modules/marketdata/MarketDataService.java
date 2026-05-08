package com.hcy.quant_core.modules.marketdata;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.IMarketDataUseCase;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MarketDataService implements IMarketDataUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final OhlcvPersistencePort persistencePort;
	private final JobLauncher jobLauncher;
	private final Job ohlcvIngestionJob;

	public MarketDataService(OhlcvPersistencePort persistencePort, JobLauncher jobLauncher,
		@Qualifier("ohlcvIngestionJob") Job ohlcvIngestionJob) {
		this.persistencePort = persistencePort;
		this.jobLauncher = jobLauncher;
		this.ohlcvIngestionJob = ohlcvIngestionJob;
	}

	@Override
	public List<OhlcvRecord> getOhlcv(String symbol, String interval) {
		return persistencePort.findBySymbolAndInterval(symbol, interval);
	}

	@Override
	public String triggerIngestion(String symbol, String interval) {

		// 第一次會拉取過去 365 天數據
		Long maxOpenTime = persistencePort.getMaxOpenTime(symbol, interval);
		long startTime = maxOpenTime != null ?
			maxOpenTime + 1 :
			LocalDate.now().minusDays(365).atStartOfDay(ZoneOffset.UTC).toInstant()
			.toEpochMilli();

		JobParameters params =
			new JobParametersBuilder().addString("symbol", symbol).addString("interval", interval)
				.addLong("startTime", startTime)
				.addLong("runAt", System.currentTimeMillis()) // 確保每次 JobParameters 唯一
				.toJobParameters();

		LOGGER.info("ohlcvIngestionJob params symbol={}, interval={}, startTime={}, runAt={}",
			symbol, interval, startTime, System.currentTimeMillis());

		try {
			JobExecution execution = jobLauncher.run(ohlcvIngestionJob, params);
			return execution.getId().toString();
		} catch (Exception e) {
			throw new QuantException("Ingestion job launch failed: " + e.getMessage(), e);
		}
	}

}

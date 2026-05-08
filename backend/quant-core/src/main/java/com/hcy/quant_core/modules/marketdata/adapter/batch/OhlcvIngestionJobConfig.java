package com.hcy.quant_core.modules.marketdata.adapter.batch;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OhlcvIngestionJobConfig {
	// Job定義
	@Bean
	public Job ohlcvIngestionJob(JobRepository jobRepository, Step ohlcvStep) {
		return new JobBuilder("ohlcvIngestionJob", jobRepository)
			.start(ohlcvStep)
			.build();
	}

	// Step定義
	@Bean
	public Step ohlcvStep(JobRepository jobRepository,
		PlatformTransactionManager txManager,
		BinanceItemReader reader,
		OhlcvItemProcessor processor,
		OhlcvItemWriter writer) {
		return new StepBuilder("ohlcvStep", jobRepository)
			.<OhlcvRecord, OhlcvRecord>chunk(500, txManager) // 每 500 筆一次 commit
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.faultTolerant()
			.skip(Exception.class)
			.skipLimit(10)
			.build();
	}

	@Bean
	@StepScope
	public BinanceItemReader binanceItemReader(
		RestTemplate restTemplate,
		@Value("#{jobParameters['symbol']}") String symbol,
		@Value("#{jobParameters['interval']}") String interval,
		@Value("#{jobParameters['startTime']}") Long startTime) {
		return new BinanceItemReader(restTemplate, symbol, interval, startTime);
	}

	@Bean
	public OhlcvItemProcessor ohlcvItemProcessor(OhlcvPersistencePort persistencePort) {
		return new OhlcvItemProcessor(persistencePort);
	}

	@Bean
	public OhlcvItemWriter ohlcvItemWriter(OhlcvPersistencePort persistencePort) {
		return new OhlcvItemWriter(persistencePort);
	}
}

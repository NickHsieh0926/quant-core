package com.hcy.quant_core.modules.onchain.adaptor.batch;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OnChainIngestionJobConfig {
	
	@Bean
	@StepScope
	public FearGreedItemReader fearGreedItemReader(RestTemplate restTemplate) {
		return new FearGreedItemReader(restTemplate);
	}

	@Bean
	public OnChainItemProcessor onChainItemProcessor(
		OnChainMetricsPersistencePort persistencePort) {
		return new OnChainItemProcessor(persistencePort);
	}

	@Bean
	public OnChainItemWriter onChainItemWriter(
		OnChainMetricsPersistencePort persistencePort) {
		return new OnChainItemWriter(persistencePort);
	}

	@Bean
	public Job onChainIngestionJob(JobRepository jobRepository, Step onChainStep) {
		return new JobBuilder("onChainIngestionJob", jobRepository)
			.start(onChainStep)
			.build();
	}

	@Bean
	public Step onChainStep(JobRepository jobRepository,
		PlatformTransactionManager txManager,
		FearGreedItemReader reader,
		OnChainItemProcessor processor,
		OnChainItemWriter writer) {
		return new StepBuilder("onChainStep", jobRepository)
			.<OnChainMetricsRecord, OnChainMetricsRecord>chunk(30, txManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}
}

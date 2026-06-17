package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Configuration
public class OnChainIngestionJobConfig {

	@Bean
	@StepScope
	public FearGreedItemReader fearGreedItemReader(RestTemplate restTemplate) {
		return new FearGreedItemReader(restTemplate);
	}

	@Bean
	@StepScope
	public ExchangeFlowItemReader exchangeFlowItemReader() {
		return new ExchangeFlowItemReader();
	}

	@Bean
	@StepScope
	public OnChainItemProcessor onChainItemProcessor(
		OnChainMetricsPersistencePort persistencePort,
		ExchangeFlowItemReader exchangeFlowItemReader) throws Exception {

		BigDecimal flow = exchangeFlowItemReader.read();
		return new OnChainItemProcessor(persistencePort, flow);
	}

	@Bean
	public OnChainItemWriter onChainItemWriter(
		OnChainMetricsPersistencePort persistencePort) {
		return new OnChainItemWriter(persistencePort);
	}

	@Bean
	public OnChainJobListener onChainJobListener(
		ApplicationEventPublisher eventPublisher) {
		return new OnChainJobListener(eventPublisher);
	}

	@Bean
	public Job onChainIngestionJob(JobRepository jobRepository, Step onChainStep,
		OnChainJobListener onChainJobListener) {
		return new JobBuilder("onChainIngestionJob", jobRepository)
			.listener(onChainJobListener)
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

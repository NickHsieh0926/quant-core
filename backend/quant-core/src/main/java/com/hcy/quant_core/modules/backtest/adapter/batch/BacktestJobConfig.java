package com.hcy.quant_core.modules.backtest.adapter.batch;

import com.hcy.quant_core.modules.backtest.model.OhlcvPair;
import com.hcy.quant_core.modules.backtest.model.TradeResult;
import com.hcy.quant_core.modules.backtest.port.BacktestResultPersistencePort;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import com.hcy.quant_core.modules.statarb.calculator.ZScoreCalculator;
import com.hcy.quant_core.modules.statarb.strategy.MeanReversionStrategy;
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

@Configuration
public class BacktestJobConfig {
	@Bean
	@StepScope
	public BacktestItemReader backtestItemReader(
		OhlcvPersistencePort persistencePort,
		@Value("#{jobParameters['symbolA']}") String symbolA,
		@Value("#{jobParameters['symbolB']}") String symbolB,
		@Value("#{jobParameters['interval']}") String interval) {
		return new BacktestItemReader(persistencePort, symbolA, symbolB, interval);
	}

	@Bean
	@StepScope
	public BacktestItemProcessor backtestItemProcessor(
		ZScoreCalculator zScoreCalculator,
		@Value("#{jobParameters['lookBackDays']}") Integer lookBackDays,
		@Value("#{jobParameters['entryZScore']}") Double entryZScore,
		@Value("#{jobParameters['exitZScore']}") Double exitZScore,
		@Value("#{jobParameters['symbolA']}") String symbolA,
		@Value("#{jobParameters['symbolB']}") String symbolB) {
		MeanReversionStrategy strategy = new MeanReversionStrategy(
			symbolA, symbolB, lookBackDays, entryZScore, exitZScore);
		return new BacktestItemProcessor(strategy, zScoreCalculator);
	}

	@Bean
	@StepScope
	public BacktestItemWriter backtestItemWriter(BacktestResultPersistencePort persistencePort) {
		return new BacktestItemWriter(persistencePort);
	}

	@Bean
	public Job backtestJob(JobRepository jobRepository, Step backtestStep) {
		return new JobBuilder("backtestJob", jobRepository)
			.start(backtestStep)
			.build();
	}

	@Bean
	public Step backtestStep(JobRepository jobRepository,
		PlatformTransactionManager txManager,
		BacktestItemReader reader,
		BacktestItemProcessor processor,
		BacktestItemWriter writer) {
		return new StepBuilder("backtestStep", jobRepository)
			.<OhlcvPair, TradeResult>chunk(100, txManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.listener(writer)
			.build();
	}
}

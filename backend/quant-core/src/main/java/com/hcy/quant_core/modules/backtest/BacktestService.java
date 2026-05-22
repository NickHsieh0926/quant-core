package com.hcy.quant_core.modules.backtest;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;
import com.hcy.quant_core.modules.backtest.port.BacktestResultPersistencePort;
import com.hcy.quant_core.modules.backtest.port.IBacktestUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BacktestService implements IBacktestUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(BacktestService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final JobLauncher jobLauncher;
	private final Job backtestJob;
	private final BacktestResultPersistencePort persistencePort;

	public BacktestService(JobLauncher jobLauncher,
		@Qualifier("backtestJob") Job backtestJob,
		BacktestResultPersistencePort persistencePort) {
		this.jobLauncher = jobLauncher;
		this.backtestJob = backtestJob;
		this.persistencePort = persistencePort;
	}

	// strategy 當所有策略實作完成後才會改為動態取值
	@Override
	public String run(String symbolA, String symbolB, int lookBackDays,
		double entryZScore, double exitZScore, String interval) {
		JobParameters params = new JobParametersBuilder()
			.addString("symbolA", symbolA)
			.addString("symbolB", symbolB)
			.addString("interval", interval)
			.addLong("lookBackDays", (long) lookBackDays)
			.addDouble("entryZScore", entryZScore)
			.addDouble("exitZScore", exitZScore)
			.addString("strategy", "MeanReversion")
			.addLong("runAt", System.currentTimeMillis())
			.toJobParameters();

		LOGGER.info(
			"BacktestJob params symbolA={}, symbolB={}, interval={}, lookBackDays={}, " +
				"entryZScore={}, exitZScore={}",
			symbolA, symbolB, interval, lookBackDays, entryZScore, exitZScore);

		try {
			JobExecution execution = jobLauncher.run(backtestJob, params);
			return execution.getJobId().toString();
		} catch (Exception e) {
			throw new QuantException("Backtest job launch failed: " + e.getMessage(), e);
		}
	}

	@Override
	public Optional<PerformanceRecord> getResult(String jobId) {
		return persistencePort.findByJobId(jobId);
	}

	@Override
	public List<PerformanceRecord> getHistory() {
		return persistencePort.findAll();
	}
}

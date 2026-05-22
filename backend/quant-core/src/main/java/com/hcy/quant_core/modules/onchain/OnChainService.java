package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnChainService implements IOnChainUseCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnChainService.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final OnChainMetricsPersistencePort persistencePort;
	private final JobLauncher jobLauncher;
	private final Job onChainIngestionJob;

	public OnChainService(OnChainMetricsPersistencePort persistencePort,
		JobLauncher jobLauncher,
		@Qualifier("onChainIngestionJob") Job onChainIngestionJob) {
		this.persistencePort = persistencePort;
		this.jobLauncher = jobLauncher;
		this.onChainIngestionJob = onChainIngestionJob;
	}

	@Override
	public List<OnChainMetricsRecord> getLatestMetrics(int limit) {
		return persistencePort.findLatest(limit);
	}

	@Override
	public List<OnChainMetricsRecord> getAllMetrics() {
		return persistencePort.findAll();
	}

	@Override
	public void triggerIngestion() {
		JobParameters params = new JobParametersBuilder()
			.addLong("runAt", System.currentTimeMillis())
			.toJobParameters();
		try {
			jobLauncher.run(onChainIngestionJob, params);
		} catch (Exception e) {
			throw new QuantException("OnChain ingestion job failed: " + e.getMessage(), e);
		}
	}
}

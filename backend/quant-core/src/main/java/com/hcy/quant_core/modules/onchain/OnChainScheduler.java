package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;
import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
import com.hcy.quant_core.modules.onchain.port.OnChainSignalPersistencePort;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.model.direction.OnChainDirection;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OnChainScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnChainScheduler.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IOnChainUseCase onChainUseCase;
	private final IAlertPublisher alertPublisher;
	private final JobLauncher jobLauncher;
	private final Job onChainIngestionJob;
	private final OnChainSignalPersistencePort persistencePort;
	private final SignalAlertPersistencePort signalAlertPersistencePort;

	// 追蹤三種狀態
	private OnChainDirection lastAlertDirection = OnChainDirection.NEUTRAL;

	public OnChainScheduler(IOnChainUseCase onChainUseCase,
		IAlertPublisher alertPublisher,
		JobLauncher jobLauncher,
		@Qualifier("onChainIngestionJob") Job onChainIngestionJob,
		OnChainSignalPersistencePort persistencePort,
		SignalAlertPersistencePort signalAlertPersistencePort) {
		this.onChainUseCase = onChainUseCase;
		this.alertPublisher = alertPublisher;
		this.jobLauncher = jobLauncher;
		this.onChainIngestionJob = onChainIngestionJob;
		this.persistencePort = persistencePort;
		this.signalAlertPersistencePort = signalAlertPersistencePort;
	}

	@Scheduled(cron = "${app.scheduler.onchain.cron:-}")
	public void scheduledRun() {
		JobParameters params = new JobParametersBuilder()
			.addLong("runAt", System.currentTimeMillis())
			.toJobParameters();
		try {
			jobLauncher.run(onChainIngestionJob, params);
		} catch (Exception e) {
			throw new QuantException("OnChain ingestion job failed: " + e.getMessage(), e);
		}
	}

	@EventListener
	public void evaluateAndPublish(OnChainJobCompletedEvent event) {
		LOGGER.info("[OnChainJob Completed]- evaluateAndPublish triggered");
		OnChainSignalRecord signal = onChainUseCase.calculateSignal();
		if (signal == null)
			return;

		persistencePort.save(signal);

		OnChainDirection currentAlertDirection = signal.triggered()
			? OnChainDirection.valueOf(signal.direction())
			: OnChainDirection.NEUTRAL;

		boolean shouldAlert = currentAlertDirection != lastAlertDirection;

		AlertType alertType = !shouldAlert ?
			AlertType.NONE :
			currentAlertDirection == OnChainDirection.NEUTRAL ? AlertType.EXIT : AlertType.ENTRY;

		// 	BULLISH -> BEARISH || BEARISH -> BULLISH
		boolean isDirectFlip = signal.triggered()
			&& lastAlertDirection != OnChainDirection.NEUTRAL
			&& currentAlertDirection != lastAlertDirection;

		if (isDirectFlip) {
			alertPublisher.publish(new SignalAlertPayload(
				"ON_CHAIN",
				"BTCUSDT",
				lastAlertDirection,
				signal.compositeScore(),
				AlertType.EXIT,
				signal.summary(),
				signal.signalAt()
			));
			saveSignalAlert(signal, lastAlertDirection, AlertType.EXIT);
		}

		OnChainDirection direction = alertType == AlertType.EXIT ? lastAlertDirection :
			currentAlertDirection;

		alertPublisher.publish(new SignalAlertPayload(
			"ON_CHAIN",
			"BTCUSDT",
			direction,
			signal.compositeScore(),
			alertType,
			signal.summary(),
			signal.signalAt()
		));

		if (alertType != AlertType.NONE) {
			saveSignalAlert(signal, direction, alertType);
		}

		lastAlertDirection = currentAlertDirection;
	}

	private void saveSignalAlert(OnChainSignalRecord signal,
		OnChainDirection direction, AlertType alertType) {
		signalAlertPersistencePort.save(new SignalAlertRecord(
			"ON_CHAIN",
			direction.name(),
			alertType.name(),
			"BTCUSDT",
			null,
			signal.symbolAPrice(),   // Step 2-3 完成後從 signal 取，不重讀 Redis
			null,
			signal.signalAt()
		));
	}
}

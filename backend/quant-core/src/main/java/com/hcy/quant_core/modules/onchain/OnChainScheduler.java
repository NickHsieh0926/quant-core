package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;
import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
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

	// 追蹤三種狀態
	private OnChainDirection lastAlertDirection = OnChainDirection.NEUTRAL;

	public OnChainScheduler(IOnChainUseCase onChainUseCase,
		IAlertPublisher alertPublisher,
		JobLauncher jobLauncher,
		@Qualifier("onChainIngestionJob") Job onChainIngestionJob) {
		this.onChainUseCase = onChainUseCase;
		this.alertPublisher = alertPublisher;
		this.jobLauncher = jobLauncher;
		this.onChainIngestionJob = onChainIngestionJob;
	}

	// 每天 00:05 自動執行"0 5 0 * * *"
	// Every 3 minutes "0 */3 * ? * *"
	@Scheduled(cron = "0 */3 * ? * *")
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
				lastAlertDirection,
				signal.compositeScore(),
				AlertType.EXIT,
				signal.summary(),
				signal.signalAt()
			));
		}

		alertPublisher.publish(new SignalAlertPayload(
			"ON_CHAIN",
			alertType == AlertType.EXIT ? lastAlertDirection : currentAlertDirection,
			signal.compositeScore(),
			alertType,
			signal.summary(),
			signal.signalAt()
		));

		lastAlertDirection = currentAlertDirection;
	}

}

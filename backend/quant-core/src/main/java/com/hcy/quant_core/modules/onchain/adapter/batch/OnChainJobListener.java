package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.modules.onchain.OnChainJobCompletedEvent;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.ApplicationEventPublisher;

public class OnChainJobListener implements JobExecutionListener {

	private final ApplicationEventPublisher eventPublisher;

	public OnChainJobListener(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			eventPublisher.publishEvent(new OnChainJobCompletedEvent(this));
		}
	}
}

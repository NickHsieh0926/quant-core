package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OnChainScheduler {
	private final IOnChainUseCase onChainUseCase;

	public OnChainScheduler(IOnChainUseCase onChainUseCase) {
		this.onChainUseCase = onChainUseCase;
	}

	// 每天 00:05 自動執行
	@Scheduled(cron = "0 5 0 * * *")
	public void scheduledRun() {
		onChainUseCase.triggerIngestion();
	}
}

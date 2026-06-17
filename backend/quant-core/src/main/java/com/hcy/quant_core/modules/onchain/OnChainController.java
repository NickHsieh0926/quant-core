package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.web.response.ApiResponse;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/onchain")
public class OnChainController {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnChainController.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IOnChainUseCase onChainUseCase;

	public OnChainController(IOnChainUseCase onChainUseCase) {
		this.onChainUseCase = onChainUseCase;
	}

	@GetMapping("/metrics")
	public ApiResponse<List<OnChainMetricsRecord>> getLatestMetrics(
		@RequestParam(defaultValue = "30") int limit) {
		TRACE.message("getLatestMetrics Request");
		List<OnChainMetricsRecord> result = onChainUseCase.getLatestMetrics(limit);
		return ApiResponse.ok(result);
	}
}

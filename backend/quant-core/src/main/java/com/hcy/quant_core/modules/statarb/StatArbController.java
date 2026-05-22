package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.web.response.ApiResponse;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/signals")
public class StatArbController {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatArbController.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IStatArbUseCase statArbUseCase;

	public StatArbController(IStatArbUseCase statArbUseCase) {
		this.statArbUseCase = statArbUseCase;
	}

	@GetMapping("/history")
	public ApiResponse<List<StatArbSignalRecord>> getHistory(
		@RequestParam(defaultValue = "10") int limit) {
		TRACE.message("getHistory Request");
		List<StatArbSignalRecord> result = statArbUseCase.getRecentSignals(limit);
		return ApiResponse.ok(result);
	}
}

package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.web.response.ApiResponse;
import com.hcy.quant_core.modules.statarb.config.StatArbProperties;
import com.hcy.quant_core.modules.statarb.model.StatArbParams;
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
	private final StatArbProperties props;

	public StatArbController(IStatArbUseCase statArbUseCase, StatArbProperties props) {
		this.statArbUseCase = statArbUseCase;
		this.props = props;
	}

	@GetMapping("/history")
	public ApiResponse<List<StatArbSignalRecord>> getHistory(
		@RequestParam(defaultValue = "10") int limit) {
		TRACE.message("getHistory Request");
		List<StatArbSignalRecord> result = statArbUseCase.getRecentSignals(limit);
		return ApiResponse.ok(result);
	}

	@GetMapping("/live")
	public ApiResponse<StatArbSignalRecord> getLive(
		@RequestParam String symbolA,
		@RequestParam String symbolB,
		@RequestParam(required = false) Double entryThreshold,
		@RequestParam(required = false) Double exitThreshold,
		@RequestParam(required = false) Integer lookbackSize) {

		StatArbProperties.PairConfig pairConfig = props.pairs().stream()
			.filter(p -> p.symbolA().equals(symbolA) && p.symbolB().equals(symbolB))
			.findFirst()
			.orElseThrow(() -> new QuantException(
				"Pair not configured: " + symbolA + "/" + symbolB));

		StatArbParams params = new StatArbParams(
			entryThreshold != null ? entryThreshold : pairConfig.zscoreThreshold(),
			exitThreshold != null ? exitThreshold : pairConfig.zscoreExitThreshold(),
			lookbackSize != null ? lookbackSize : pairConfig.lookbackSize()
		);

		LOGGER.info("getLive Param -> symbolA:{}, symbolB:{}, entryThreshold:{}, " +
				"exitThreshold:{}, lookbackSize:{}", symbolA, symbolB, params.entryThreshold(),
			params.exitThreshold(), params.lookbackSize());

		StatArbSignalRecord signal = statArbUseCase.calculate(symbolA, symbolB, params);
		return ApiResponse.ok(signal);
	}
}

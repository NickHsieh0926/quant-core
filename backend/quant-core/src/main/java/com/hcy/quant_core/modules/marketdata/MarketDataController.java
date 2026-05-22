package com.hcy.quant_core.modules.marketdata;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.web.response.ApiResponse;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.model.dto.IngestionRequest;
import com.hcy.quant_core.modules.marketdata.port.IMarketDataUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataController.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IMarketDataUseCase marketDataUseCase;

	public MarketDataController(IMarketDataUseCase marketDataUseCase) {
		this.marketDataUseCase = marketDataUseCase;
	}

	@GetMapping("/ohlcv")
	public ApiResponse<List<OhlcvRecord>> getOhlcv(@RequestParam String symbol,
		@RequestParam String interval) {
		TRACE.message("getOhlcv Request");
		List<OhlcvRecord> result = marketDataUseCase.getOhlcv(symbol, interval);
		return ApiResponse.ok(result);
	}

	@PostMapping("/ingest")
	public ResponseEntity<ApiResponse<String>> triggerIngestion(
		@RequestBody IngestionRequest request) {
		TRACE.message("triggerIngestion Request");
		String jobId = marketDataUseCase.triggerIngestion(request.symbol(), request.interval());
		return ResponseEntity.accepted().body(ApiResponse.ok("TriggerIngestion started", jobId));
	}
}

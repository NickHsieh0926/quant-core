package com.hcy.quant_core.modules.marketdata;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.marketdata.model.IngestionRequest;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
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
	public List<OhlcvRecord> getOhlcv(@RequestParam String symbol,
		@RequestParam String interval) {
		TRACE.message("[MarketDataController] getOhlcv Request");
		return marketDataUseCase.getOhlcv(symbol, interval);
	}

	@PostMapping("/ingest")
	public ResponseEntity<String> triggerIngestion(@RequestBody IngestionRequest request) {
		TRACE.message("[MarketDataController] triggerIngestion Request");
		String jobId = marketDataUseCase.triggerIngestion(request.symbol(), request.interval());
		return ResponseEntity.accepted().body("Job started, id=" + jobId);
	}
}

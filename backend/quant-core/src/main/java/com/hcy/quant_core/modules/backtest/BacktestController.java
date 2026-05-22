package com.hcy.quant_core.modules.backtest;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.web.response.ApiResponse;
import com.hcy.quant_core.infrastructure.web.response.JobStatusResponse;
import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;
import com.hcy.quant_core.modules.backtest.model.dto.BacktestRequest;
import com.hcy.quant_core.modules.backtest.port.IBacktestUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {
	private static final Logger LOGGER = LoggerFactory.getLogger(BacktestController.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final IBacktestUseCase backtestUseCase;

	public BacktestController(IBacktestUseCase backtestUseCase) {
		this.backtestUseCase = backtestUseCase;
	}

	@PostMapping("/run")
	public ResponseEntity<ApiResponse<String>> runBacktest(@RequestBody BacktestRequest request) {
		LOGGER.info("Run Backtest Request");
		String jobId = backtestUseCase.run(
			request.symbolA(), request.symbolB(), request.lookBackDays(),
			request.entryZScore(), request.exitZScore(), request.interval());
		return ResponseEntity.accepted().body(ApiResponse.ok("Backtest started", jobId));
	}

	@GetMapping("/result/{jobId}")
	public ResponseEntity<ApiResponse<JobStatusResponse<PerformanceRecord>>> getResult(
		@PathVariable String jobId) {
		LOGGER.info("Get Backtest Result Request jobId = {}", jobId);
		return backtestUseCase.getResult(jobId)
			.map(r -> ResponseEntity.ok(
				ApiResponse.ok(JobStatusResponse.completed(r))))
			.orElse(ResponseEntity.accepted()
				.body(ApiResponse.ok(JobStatusResponse.processing())));
	}

	@GetMapping("/history")
	public ApiResponse<List<PerformanceRecord>> getHistory() {
		LOGGER.info("Get Backtest History Request");
		List<PerformanceRecord> result = backtestUseCase.getHistory();
		return ApiResponse.ok(result);
	}
}

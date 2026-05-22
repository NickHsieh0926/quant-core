package com.hcy.quant_core.modules.backtest.adapter.batch;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;
import com.hcy.quant_core.modules.backtest.model.TradeResult;
import com.hcy.quant_core.modules.backtest.port.BacktestResultPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class BacktestItemWriter implements ItemWriter<TradeResult>, StepExecutionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(BacktestItemWriter.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final BacktestResultPersistencePort persistencePort;
	private final List<TradeResult> accumulated = new ArrayList<>();
	private String jobId;
	private String strategy;
	private String symbolA;
	private String symbolB;

	public BacktestItemWriter(BacktestResultPersistencePort persistencePort) {
		this.persistencePort = persistencePort;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		jobId = stepExecution.getJobExecution().getJobId().toString();
		strategy = stepExecution.getJobParameters().getString("strategy", "MeanReversion");
		symbolA = stepExecution.getJobParameters().getString("symbolA", "BTCUSDT");
		symbolB = stepExecution.getJobParameters().getString("symbolB", "ETHUSDT");
	}

	@Override
	public void write(Chunk<? extends TradeResult> chunk) {
		accumulated.addAll(chunk.getItems());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (!accumulated.isEmpty()) {
			PerformanceRecord performance = calculatePerformance();
			persistencePort.save(performance);
		}
		return null; // 為了不覆蓋 Step 的原始 ExitStatus
	}

	private PerformanceRecord calculatePerformance() {
		int total = accumulated.size();

		LOGGER.info("accumulated.size() = {}", accumulated.size());

		// 回測日期範圍
		LocalDate startDate = accumulated.stream()
			.mapToLong(TradeResult::openTime)
			.min()
			.stream()
			.mapToObj(t -> Instant.ofEpochMilli(t).atZone(ZoneId.of("UTC")).toLocalDate())
			.findFirst()
			.orElse(LocalDate.now().minusDays(365));

		LocalDate endDate = accumulated.stream()
			.mapToLong(TradeResult::openTime)
			.max()
			.stream()
			.mapToObj(t -> Instant.ofEpochMilli(t).atZone(ZoneId.of("UTC")).toLocalDate())
			.findFirst()
			.orElse(LocalDate.now());

		LOGGER.info("accumulated.size() = {} ,startDate={} ,startDate={}", accumulated.size(),
			startDate, endDate);

		// 以下數值計算為mock用，正確邏輯Phase2實作
		long wins = accumulated.stream()
			.filter(t -> "CLOSE".equals(t.action()))
			.count();
		double winRate = total > 0 ? (double) wins / total : 0;

		double sharpe = winRate > 0 && winRate < 1 ? winRate / (1 - winRate) : 0;

		long stopLosses = accumulated.stream()
			.filter(t -> "STOP_LOSS".equals(t.action()))
			.count();
		double maxDrawdown = total > 0 ? (double) stopLosses / total : 0;

		return new PerformanceRecord(
			jobId,
			strategy,
			symbolA,
			symbolB,
			startDate,
			endDate,
			sharpe,
			maxDrawdown,
			winRate,
			0.0,  // annualReturn：Phase 2 補強（需要實際資金模擬）
			total
		);
	}
}

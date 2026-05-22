package com.hcy.quant_core.modules.backtest.adapter.persistence;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.backtest.model.PerformanceRecord;
import com.hcy.quant_core.modules.backtest.model.entity.BacktestResultEntity;
import com.hcy.quant_core.modules.backtest.port.BacktestResultPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class BacktestResultJpaAdapter implements BacktestResultPersistencePort {
	private static final Logger LOGGER = LoggerFactory.getLogger(BacktestResultJpaAdapter.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final BacktestResultRepository repository;

	public BacktestResultJpaAdapter(BacktestResultRepository repository) {
		this.repository = repository;
	}


	@Override
	public void save(PerformanceRecord record) {
		repository.save(toEntity(record));
	}

	@Override
	public Optional<PerformanceRecord> findByJobId(String jobId) {
		return repository.findByJobId(jobId).map(this::toRecord);
	}

	@Override
	public List<PerformanceRecord> findAll() {
		return repository.findAll().stream().map(this::toRecord).toList();
	}

	private BacktestResultEntity toEntity(PerformanceRecord r) {
		BacktestResultEntity e = new BacktestResultEntity();
		e.setJobId(r.jobId());
		e.setStrategy(r.strategy());
		e.setSymbolA(r.symbolA());
		e.setSymbolB(r.symbolB());
		e.setStartDate(r.startDate());
		e.setEndDate(r.endDate());
		e.setSharpeRatio(BigDecimal.valueOf(r.sharpeRatio()));
		e.setMaxDrawdown(BigDecimal.valueOf(r.maxDrawdown()));
		e.setWinRate(BigDecimal.valueOf(r.winRate()));
		e.setAnnualReturn(BigDecimal.valueOf(r.annualReturn()));
		e.setTotalTrades(r.totalTrades());
		e.setCreatedAt(LocalDateTime.now());
		return e;
	}

	private PerformanceRecord toRecord(BacktestResultEntity e) {
		return new PerformanceRecord(
			e.getJobId(),
			e.getStrategy(),
			e.getSymbolA(),
			e.getSymbolB(),
			e.getStartDate(),
			e.getEndDate(),
			e.getSharpeRatio() != null ? e.getSharpeRatio().doubleValue() : 0,
			e.getMaxDrawdown() != null ? e.getMaxDrawdown().doubleValue() : 0,
			e.getWinRate() != null ? e.getWinRate().doubleValue() : 0,
			e.getAnnualReturn() != null ? e.getAnnualReturn().doubleValue() : 0,
			e.getTotalTrades() != null ? e.getTotalTrades() : 0
		);
	}
}

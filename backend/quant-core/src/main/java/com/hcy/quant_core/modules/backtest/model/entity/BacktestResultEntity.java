package com.hcy.quant_core.modules.backtest.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "backtest_result")
public class BacktestResultEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "job_id", nullable = false, unique = true)
	private String jobId;

	@Column(nullable = false)
	private String strategy;

	@Column(name = "symbol_a", nullable = false)
	private String symbolA;

	@Column(name = "symbol_b", nullable = false)
	private String symbolB;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "sharpe_ratio")
	private BigDecimal sharpeRatio;

	@Column(name = "max_drawdown")
	private BigDecimal maxDrawdown;

	@Column(name = "win_rate")
	private BigDecimal winRate;

	@Column(name = "annual_return")
	private BigDecimal annualReturn;

	@Column(name = "total_trades")
	private Integer totalTrades;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;
}

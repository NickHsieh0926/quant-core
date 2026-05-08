package com.hcy.quant_core.modules.marketdata.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ohlcv")
public class OhlcvEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String symbol;

	@Column(name = "open_time", nullable = false)
	private Long openTime;

	@Column(nullable = false)
	private BigDecimal open;

	@Column(nullable = false)
	private BigDecimal high;

	@Column(nullable = false)
	private BigDecimal low;

	@Column(nullable = false)
	private BigDecimal close;

	@Column(nullable = false)
	private BigDecimal volume;

	//SQL 保留字，必須顯式聲明 mapping
	@Column(name = "interval", nullable = false)
	private String interval;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}

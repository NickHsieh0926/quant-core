package com.hcy.quant_core.modules.onchain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "on_chain_metrics")
public class OnChainMetricsEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "recorded_at", nullable = false, unique = true)
	private LocalDateTime recordedAt;

	@Column(name = "fear_greed_index")
	private Integer fearGreedIndex;

	@Column(name = "fear_greed_label")
	private String fearGreedLabel;

	@Column(name = "btc_exchange_flow")
	private BigDecimal btcExchangeFlow;

	@Column
	private BigDecimal nupl;

	@Column
	private BigDecimal sopr;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;
}

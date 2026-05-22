package com.hcy.quant_core.modules.statarb.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stat_arb_signal")
public class StatArbSignalEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "symbol_a", nullable = false)
	private String symbolA;

	@Column(name = "symbol_b", nullable = false)
	private String symbolB;

	@Column(name = "z_score", nullable = false)
	private BigDecimal zScore;

	@Column(nullable = false)
	private String direction;

	@Column(name = "signal_at", nullable = false)
	private LocalDateTime signalAt;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;
}

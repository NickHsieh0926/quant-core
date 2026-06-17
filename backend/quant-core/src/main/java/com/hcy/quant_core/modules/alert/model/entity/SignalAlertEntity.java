package com.hcy.quant_core.modules.alert.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "signal_alert")
public class SignalAlertEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String strategy;

	@Column(nullable = false, length = 20)
	private String direction;

	@Column(name = "alert_type", nullable = false, length = 10)
	private String alertType;

	@Column(name = "symbol_a", nullable = false, length = 20)
	private String symbolA;

	@Column(name = "symbol_b", length = 20)
	private String symbolB;

	@Column(name = "symbol_a_price", precision = 20, scale = 8, nullable = false)
	private BigDecimal symbolAPrice;

	@Column(name = "symbol_b_price", precision = 20, scale = 8)
	private BigDecimal symbolBPrice;

	@Column(name = "signal_at", nullable = false)
	private LocalDateTime signalAt;
}

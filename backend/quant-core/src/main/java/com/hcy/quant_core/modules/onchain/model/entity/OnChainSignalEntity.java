package com.hcy.quant_core.modules.onchain.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "on_chain_signal")
public class OnChainSignalEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private LocalDateTime signalAt;

	private Integer fearGreedIndex;

	private String fearGreedLabel;

	private Integer compositeScore;

	@Column(length = 20)
	private String direction;

	private Boolean triggered;

	@Column(length = 20)
	private String source;

	@Column(name = "symbol_a_price", nullable = false)
	private BigDecimal symbolAPrice;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;
}

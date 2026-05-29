package com.hcy.quant_core.modules.onchain.adapter.persistence;

import com.hcy.quant_core.modules.onchain.model.entity.OnChainMetricsEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OnChainMetricsRepository extends JpaRepository<OnChainMetricsEntity, Long> {
	boolean existsByRecordedAt(LocalDateTime recordedAt);

	List<OnChainMetricsEntity> findAllByOrderByRecordedAtDesc(Pageable pageable);

	Optional<OnChainMetricsEntity> findTopByOrderByRecordedAtDesc();
}

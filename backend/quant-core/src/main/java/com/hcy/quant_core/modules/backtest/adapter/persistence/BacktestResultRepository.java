package com.hcy.quant_core.modules.backtest.adapter.persistence;

import com.hcy.quant_core.modules.backtest.model.entity.BacktestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BacktestResultRepository extends JpaRepository<BacktestResultEntity, Long> {
	Optional<BacktestResultEntity> findByJobId(String jobId);
}

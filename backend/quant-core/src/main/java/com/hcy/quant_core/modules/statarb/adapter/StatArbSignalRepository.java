package com.hcy.quant_core.modules.statarb.adapter;

import com.hcy.quant_core.modules.statarb.model.entity.StatArbSignalEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatArbSignalRepository extends JpaRepository<StatArbSignalEntity, Long> {
	List<StatArbSignalEntity> findAllByOrderBySignalAtDesc(Pageable pageable);
}

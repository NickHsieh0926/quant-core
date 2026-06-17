package com.hcy.quant_core.modules.onchain.adapter.persistence;

import com.hcy.quant_core.modules.onchain.model.entity.OnChainSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnChainSignalRepository extends JpaRepository<OnChainSignalEntity, Long> {
}

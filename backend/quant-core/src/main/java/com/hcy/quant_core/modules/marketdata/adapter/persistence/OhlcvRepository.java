package com.hcy.quant_core.modules.marketdata.adapter.persistence;

import com.hcy.quant_core.modules.marketdata.model.entity.OhlcvEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OhlcvRepository extends JpaRepository<OhlcvEntity, Long> {
	List<OhlcvEntity> findBySymbolAndIntervalOrderByOpenTimeAsc(String symbol, String interval);

	boolean existsBySymbolAndOpenTimeAndInterval(String symbol, long openTime, String interval);

	@Query("SELECT MAX(e.openTime) FROM OhlcvEntity e WHERE e.symbol = :symbol " +
		"AND e.interval = :interval")
	Long getMaxOpenTime(@Param("symbol") String symbol, @Param("interval") String interval);
}

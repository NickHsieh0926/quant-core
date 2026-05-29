package com.hcy.quant_core.modules.onchain.adapter.persistence;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.model.entity.OnChainMetricsEntity;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OnChainMetricsJpaAdapter implements OnChainMetricsPersistencePort {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnChainMetricsJpaAdapter.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final OnChainMetricsRepository repository;

	public OnChainMetricsJpaAdapter(OnChainMetricsRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(OnChainMetricsRecord record) {
		repository.save(toEntity(record));
	}

	@Override
	public boolean existsByRecordedAt(LocalDateTime recordedAt) {
		return repository.existsByRecordedAt(recordedAt);
	}

	@Override
	public List<OnChainMetricsRecord> findLatest(int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit);
		return repository.findAllByOrderByRecordedAtDesc(pageRequest)
			.stream()
			.map(this::toRecord)
			.toList();
	}

	@Override
	public List<OnChainMetricsRecord> findAll() {
		return repository.findAll().stream().map(this::toRecord).toList();
	}

	@Override
	public OnChainMetricsRecord findLatestOne() {
		return repository.findTopByOrderByRecordedAtDesc()
			.map(this::toRecord)
			.orElse(null);
	}

	private OnChainMetricsEntity toEntity(OnChainMetricsRecord r) {
		OnChainMetricsEntity e = new OnChainMetricsEntity();
		e.setRecordedAt(r.recordedAt());
		e.setFearGreedIndex(r.fearGreedIndex());
		e.setFearGreedLabel(r.fearGreedLabel());
		e.setBtcExchangeFlow(r.btcExchangeFlow());
		e.setNupl(r.nupl());
		e.setSopr(r.sopr());
		return e;
	}

	private OnChainMetricsRecord toRecord(OnChainMetricsEntity e) {
		return new OnChainMetricsRecord(
			e.getRecordedAt(), e.getFearGreedIndex(), e.getFearGreedLabel(),
			e.getBtcExchangeFlow(), e.getNupl(), e.getSopr()
		);
	}
}

package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class OnChainItemProcessor
	implements ItemProcessor<OnChainMetricsRecord, OnChainMetricsRecord> {
	private final OnChainMetricsPersistencePort persistencePort;
	private final BigDecimal todayExchangeFlow;

	public OnChainItemProcessor(OnChainMetricsPersistencePort persistencePort,
		BigDecimal todayExchangeFlow) {
		this.persistencePort = persistencePort;
		this.todayExchangeFlow = todayExchangeFlow;
	}

	@Override
	public OnChainMetricsRecord process(OnChainMetricsRecord record) {
		if (persistencePort.existsByRecordedAt(record.recordedAt())) {
			return null;
		}

		// MOCK 先只存最新一筆 flow 數值
		boolean isToday = record.recordedAt().toLocalDate().equals(LocalDate.now(ZoneOffset.UTC));
		BigDecimal flow = isToday ? todayExchangeFlow : null;

		return new OnChainMetricsRecord(
			record.recordedAt(),
			record.fearGreedIndex(),
			record.fearGreedLabel(),
			flow,
			record.nupl(),
			record.sopr()
		);
	}
}

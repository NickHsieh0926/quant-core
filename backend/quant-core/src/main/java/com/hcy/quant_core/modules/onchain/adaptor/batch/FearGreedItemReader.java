package com.hcy.quant_core.modules.onchain.adaptor.batch;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FearGreedItemReader implements ItemReader<OnChainMetricsRecord> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FearGreedItemReader.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final RestTemplate restTemplate;
	private final Queue<OnChainMetricsRecord> buffer = new LinkedList<>();
	private boolean fetched = false;

	public FearGreedItemReader(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public OnChainMetricsRecord read() {
		TRACE.message("buffer.isEmpty={}, fetched={}", buffer.isEmpty(), fetched);
		if (!fetched) {
			fetchData();
			fetched = true;
		}
		return buffer.poll();
	}

	private void fetchData() {
		String url = "https://api.alternative.me/fng/?limit=30&format=json";
		FearGreedResponse response = restTemplate.getForObject(url, FearGreedResponse.class);

		if (response == null || response.data() == null)
			return;

		for (FearGreedData d : response.data()) {
			buffer.add(new OnChainMetricsRecord(
				LocalDateTime.ofEpochSecond(Long.parseLong(d.timestamp()), 0, ZoneOffset.UTC),
				Integer.parseInt(d.value()),
				d.value_classification(),
				null,
				null,
				null
			));
		}
	}

	// 對應 Alternative.me API 回傳結構的 Record
	record FearGreedResponse(List<FearGreedData> data) {}
	record FearGreedData(String value, String value_classification, String timestamp) {}
}

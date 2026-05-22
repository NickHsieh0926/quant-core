package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.modules.onchain.adaptor.batch.FearGreedItemReader;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class FearGreedItemReaderTest {
	private final RestTemplate mockTemplate = mock(RestTemplate.class);

	@Test
	void read_whenApiReturnsNull_returnsNullSafely() throws Exception {
		FearGreedItemReader reader = new FearGreedItemReader(mockTemplate);
		when(mockTemplate.getForObject(anyString(), any())).thenReturn(null);

		OnChainMetricsRecord result = reader.read();

		assertThat(result).isNull();
		verify(mockTemplate, times(1)).getForObject(anyString(), any());
	}

	@Test
	void read_calledTwice_doesNotCallApiTwice() throws Exception {
		// fetched flag 確保 API 只被呼叫一次，第二次 read() 直接從空 buffer 取 null
		FearGreedItemReader reader = new FearGreedItemReader(mockTemplate);
		when(mockTemplate.getForObject(anyString(), any())).thenReturn(null);

		reader.read();
		reader.read();

		verify(mockTemplate, times(1)).getForObject(anyString(), any());
	}
}

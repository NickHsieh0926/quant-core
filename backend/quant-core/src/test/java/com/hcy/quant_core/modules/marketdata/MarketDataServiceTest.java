package com.hcy.quant_core.modules.marketdata;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class MarketDataServiceTest {
	private final OhlcvPersistencePort mockPort = mock(OhlcvPersistencePort.class);
	private final JobLauncher mockLauncher = mock(JobLauncher.class);
	private final Job mockJob = mock(Job.class);
	private final MarketDataService service =
		new MarketDataService(mockPort, mockLauncher, mockJob);

	@Test
	void getOhlcv_delegatesToPort() {
		List<OhlcvRecord> expected = List.of(
			new OhlcvRecord("BTCUSDT", 1672531200000L,
				new BigDecimal("16500"), new BigDecimal("16600"),
				new BigDecimal("16400"), new BigDecimal("16550"),
				new BigDecimal("1234"), "1d")
		);
		when(mockPort.findBySymbolAndInterval("BTCUSDT", "1d")).thenReturn(expected);

		List<OhlcvRecord> result = service.getOhlcv("BTCUSDT", "1d");

		assertThat(result).isEqualTo(expected);
		verify(mockPort).findBySymbolAndInterval("BTCUSDT", "1d");
	}

}

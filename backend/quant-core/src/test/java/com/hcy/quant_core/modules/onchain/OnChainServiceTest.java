package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OnChainServiceTest {
	private final OnChainMetricsPersistencePort mockPort =
		mock(OnChainMetricsPersistencePort.class);
	private final JobLauncher mockLauncher = mock(JobLauncher.class);
	private final Job mockJob = mock(Job.class);
	private final OnChainService service =
		new OnChainService(mockPort, mockLauncher, mockJob);

	@Test
	void getLatestMetrics_delegatesToPort() {
		List<OnChainMetricsRecord> expected = List.of(
			new OnChainMetricsRecord(LocalDateTime.now(), 35, "Fear", null, null, null)
		);
		when(mockPort.findLatest(10)).thenReturn(expected);

		List<OnChainMetricsRecord> result = service.getLatestMetrics(10);

		assertThat(result).isEqualTo(expected);
		verify(mockPort).findLatest(10);
	}
}

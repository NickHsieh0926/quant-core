package com.hcy.quant_core.modules.onchain.adapter.batch;

import com.hcy.quant_core.BaseIntegrationTest;
import com.hcy.quant_core.modules.marketdata.adapter.persistence.OhlcvRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@SpringBatchTest
public class OhlcvIngestionJobTest extends BaseIntegrationTest {

	@MockitoBean
	private RestTemplate restTemplate;

	@Autowired
	private Job ohlcvIngestionJob;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private OhlcvRepository ohlcvRepository;

	private static final Object[][] MOCK_KLINES = {
		{1704067200000L, "42000.00", "43000.00", "41000.00", "42500.00", "100.00"},
		{1704153600000L, "42500.00", "44000.00", "42000.00", "43000.00", "150.00"},
		{1704240000000L, "43000.00", "43500.00", "42500.00", "43200.00", "120.00"}
	};

	@BeforeEach
	void setUp() {
		jobLauncherTestUtils.setJob(ohlcvIngestionJob);
		ohlcvRepository.deleteAll();
		when(restTemplate.getForObject(anyString(), eq(Object[][].class), anyMap()))
			.thenReturn(MOCK_KLINES);
	}

	// Job 完成，DB 寫入正確筆數
	@Test
	void ohlcvIngestionJob_shouldCompleteSuccessfully() throws Exception {
		JobExecution execution = jobLauncherTestUtils.launchJob(
			new JobParametersBuilder()
				.addString("symbol", "BTCUSDT")
				.addString("interval", "1d")
				.addLong("startTime", 1704067200000L)
				.addLong("runAt", System.currentTimeMillis())
				.toJobParameters()
		);

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(ohlcvRepository.count()).isEqualTo(3L);
	}

	// 冪等性（相同 symbol + startTime 跑兩次，DB 筆數不增加）
	@Test
	void ohlcvIngestionJob_shouldBeIdempotent() throws Exception {
		JobParameters params1 = new JobParametersBuilder()
			.addString("symbol", "ETHUSDT")
			.addString("interval", "1d")
			.addLong("startTime", 1704067200000L)
			.addLong("runAt", 1L)
			.toJobParameters();

		JobParameters params2 = new JobParametersBuilder()
			.addString("symbol", "ETHUSDT")
			.addString("interval", "1d")
			.addLong("startTime", 1704067200000L)
			.addLong("runAt", 2L)
			.toJobParameters();

		jobLauncherTestUtils.launchJob(params1);
		long countAfterFirst = ohlcvRepository.count();

		jobLauncherTestUtils.launchJob(params2);
		long countAfterSecond = ohlcvRepository.count();

		// 第二次跑完，筆數不應增加（Processor 冪等過濾 + DB UNIQUE 約束雙重保護）
		assertThat(countAfterSecond).isEqualTo(countAfterFirst);
	}
}

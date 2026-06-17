package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.infrastructure.shared.util.RedisPriceReader;
import com.hcy.quant_core.modules.onchain.config.OnChainProperties;
import com.hcy.quant_core.modules.onchain.model.OnChainMetricsRecord;
import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;
import com.hcy.quant_core.modules.onchain.port.OnChainMetricsPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnChainServiceTest {

	@Mock
	private OnChainMetricsPersistencePort mockPort;
	@Mock
	private RedisPriceReader redisPriceReader;
	@Mock
	private ValueOperations<String, String> valueOps;

	private final OnChainProperties properties =
		new OnChainProperties(
			new OnChainProperties.CompositeScore(
				25,
				40,
				60,
				75,
				20,
				10,
				-10,
				-20,
				-5000.0,
				5000.0,
				15,
				5,
				-5,
				-15,
				75,
				25
			)
		);


	private OnChainService service;

	@BeforeEach
	void setUp() {
		service = new OnChainService(mockPort, redisPriceReader, properties);
	}

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

	@Test
	void calculateSignal_whenNoData_returnsNull() {
		when(mockPort.findLatestOne()).thenReturn(null);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result).isNull();
	}

	// fearGreed = 24（< 25  score+20）→ score 70，NOT triggered
	@Test
	void calculateSignal_fearGreed24_score70_notTriggered() {
		when(mockPort.findLatestOne()).thenReturn(
			new OnChainMetricsRecord(LocalDateTime.now(), 24, "Extreme Fear", null, null, null)
		);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result).isNotNull();
		assertThat(result.compositeScore()).isEqualTo(70);
		assertThat(result.direction()).isEqualTo("NEUTRAL");
		assertThat(result.triggered()).isFalse();
	}

	// fearGreed = 25（< 40  score +10）→ score 60
	@Test
	void calculateSignal_fearGreed25_score60_directionBullish() {
		when(mockPort.findLatestOne()).thenReturn(
			new OnChainMetricsRecord(LocalDateTime.now(), 25, "Fear", null, null, null)
		);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result.compositeScore()).isEqualTo(60);
		assertThat(result.direction()).isEqualTo("NEUTRAL");
		assertThat(result.triggered()).isFalse();
	}

	// fearGreed = 75（> 60  score -10）→ score 40
	@Test
	void calculateSignal_fearGreed75_score40_directionBearish() {
		when(mockPort.findLatestOne()).thenReturn(
			new OnChainMetricsRecord(LocalDateTime.now(), 75, "Greed", null, null, null)
		);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result.compositeScore()).isEqualTo(40);
		assertThat(result.direction()).isEqualTo("NEUTRAL");
		assertThat(result.triggered()).isFalse();
	}

	// fearGreed = 76（> 75  score -20）→ score 30，NOT triggered
	@Test
	void calculateSignal_fearGreed76_score30_notTriggered() {
		when(mockPort.findLatestOne()).thenReturn(
			new OnChainMetricsRecord(LocalDateTime.now(), 76, "Extreme Greed", null, null, null)
		);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result.compositeScore()).isEqualTo(30);
		assertThat(result.direction()).isEqualTo("NEUTRAL");
		assertThat(result.triggered()).isFalse();
	}

	// fearGreed = 24（+20）+ exchangeFlow = -1 → score 75，triggered = true
	@Test
	void calculateSignal_fearGreed24WithNegativeFlow_score75_triggered() {
		when(mockPort.findLatestOne()).thenReturn(
			new OnChainMetricsRecord(LocalDateTime.now(), 24, "Extreme Fear",
				BigDecimal.valueOf(-6000), null, null)
		);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result.compositeScore()).isEqualTo(85);
		assertThat(result.direction()).isEqualTo("BULLISH");
		assertThat(result.triggered()).isTrue();
	}

	// fearGreed = 76（-20）+ exchangeFlow = 1 → score 25，triggered = true
	@Test
	void calculateSignal_fearGreed76WithPositiveFlow_score25_triggered() {
		when(mockPort.findLatestOne()).thenReturn(
			new OnChainMetricsRecord(LocalDateTime.now(), 76, "Extreme Greed",
				BigDecimal.valueOf(6000), null, null)
		);

		OnChainSignalRecord result = service.calculateSignal();

		assertThat(result.compositeScore()).isEqualTo(15);
		assertThat(result.direction()).isEqualTo("BEARISH");
		assertThat(result.triggered()).isTrue();
	}
}

package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.infrastructure.shared.util.CacheKeyConstants;
import com.hcy.quant_core.infrastructure.shared.util.RedisPriceReader;
import com.hcy.quant_core.modules.statarb.calculator.ZScoreCalculator;
import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StatArbServiceTest {

	@Mock
	private StatArbSignalPersistencePort persistencePort;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ZScoreCalculator zScoreCalculator;
	@Mock
	private ListOperations<String, String> listOps;
	@Mock
	private ValueOperations<String, String> valueOps;
	@Mock
	private RedisPriceReader redisPriceReader;

	private StatArbService service;

	private static final StatArbParams PARAMS = new StatArbParams(2.0, 0.5, 3.5, 30);

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
		service = new StatArbService(persistencePort, redisTemplate, zScoreCalculator,
			redisPriceReader);
	}

	// stub Redis 回傳 30 筆假收盤價（BTCUSDT 和 ETHUSDT 各一份）
	private void stubThirtyPrices() {
		List<String> prices = Collections.nCopies(30, "100.00");
		when(listOps.range(
			eq(CacheKeyConstants.closedPriceList("BTCUSDT")), eq(0L), eq(29L)
		)).thenReturn(prices);
		when(listOps.range(
			eq(CacheKeyConstants.closedPriceList("ETHUSDT")), eq(0L), eq(29L)
		)).thenReturn(prices);
	}

	@Test
	void whenInsufficientData() {
		// Redis 回傳空 List：模擬 WebSocket 剛啟動，收盤價還在累積
		when(listOps.range(any(), anyLong(), anyLong())).thenReturn(List.of());

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result).isNull();
		verify(zScoreCalculator, never()).calculate(any(), any());
	}

	@Test
	void whenPositiveZScoreAboveThreshold() {
		// zScore = +2.5 → |z| > 2.0，zScore > 0 → OPEN_LONG_B
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(2.5);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("OPEN_LONG_B");
	}

	@Test
	void calculate_whenNegativeZScoreAboveThreshold_shouldReturnOpenShortBAndTriggered() {
		// zScore = -2.5 → |z| > 2.0，zScore < 0 → OPEN_SHORT_B
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(-2.5);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("OPEN_SHORT_B");
	}

	@Test
	void calculate_whenZScoreBelowExitThreshold_shouldReturnCloseAndNotTriggered() {
		// zScore = +0.3 → |z| < 0.5 → 均值回歸完成，應平倉
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(0.3);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("CLOSE");
	}

	@Test
	void calculate_whenZScoreInHoldZone_shouldReturnHoldAndNotTriggered() {
		// zScore = +1.0 → 0.5 ≤ |z| ≤ 2.0 → 中性觀望區間
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(1.0);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("HOLD");
	}

	@Test
	void calculate_whenZScoreExceedsStopLoss_shouldReturnStopLoss() {
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(3.6);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("STOP_LOSS");
	}

	@Test
	void calculate_whenNegativeZScoreExceedsStopLoss_shouldReturnStopLoss() {
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(-3.6);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("STOP_LOSS");
	}

	@Test
	void calculate_whenZScoreEqualsStopLoss_shouldReturnStopLoss() {
		// 驗證 >= 而不是 >：剛好等於停損線也要觸發
		stubThirtyPrices();
		when(zScoreCalculator.calculate(any(), any())).thenReturn(3.5);

		StatArbSignalRecord result = service.calculate("BTCUSDT", "ETHUSDT", PARAMS);

		assertThat(result.direction()).isEqualTo("STOP_LOSS");
	}
}

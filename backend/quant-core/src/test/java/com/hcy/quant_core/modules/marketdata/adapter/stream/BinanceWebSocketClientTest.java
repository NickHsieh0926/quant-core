package com.hcy.quant_core.modules.marketdata.adapter.stream;

import com.hcy.quant_core.infrastructure.shared.util.CacheKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BinanceWebSocketClientTest {

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOps;
	@Mock
	private ListOperations<String, String> listOps;

	private BinanceWebSocketClient client;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
		lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
		client = new BinanceWebSocketClient("btcusdt", redisTemplate);
	}

	@Test
	void whenKlineNodeIsNull() {
		String msg = """
			{"e": "subscribeResponse"}
			""";

		client.onMessage(msg);

		verify(redisTemplate, never()).opsForValue();
		verify(redisTemplate, never()).opsForList();
	}

	@Test
	void whenKlineIsNotClosed() {
		String msg = """
			{"k": {"c": "65000.00", "x": false}}
			""";

		client.onMessage(msg);

		verify(valueOps).set(
			eq(CacheKeyConstants.latestPrice("btcusdt")),
			eq("65000.00"),
			any(Duration.class)
		);

		verify(listOps, never()).leftPush(any(), any(String.class));
	}

	@Test
	void whenKlineIsClosed() {
		String msg = """
			{"k": {"c": "65100.00", "x": true}}
			""";

		client.onMessage(msg);

		verify(valueOps).set(
			eq(CacheKeyConstants.latestPrice("btcusdt")),
			eq("65100.00"),
			any(Duration.class)
		);
		verify(listOps).leftPush(
			eq(CacheKeyConstants.closedPriceList("btcusdt")),
			eq("65100.00")
		);
		verify(listOps).trim(
			eq(CacheKeyConstants.closedPriceList("btcusdt")),
			eq(0L), eq(59L)
		);
	}

	@Test
	void whenJsonIsMalformed() {
		// 解析失敗不應報錯而中斷 Stream
		assertThatCode(() -> client.onMessage("not-valid-json"))
			.doesNotThrowAnyException();
	}
}

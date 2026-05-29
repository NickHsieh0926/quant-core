package com.hcy.quant_core.modules.marketdata.adapter.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcy.quant_core.infrastructure.shared.util.CacheKeyConstants;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@ClientEndpoint
public class BinanceWebSocketClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(BinanceWebSocketClient.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final String symbol;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public BinanceWebSocketClient(String symbol, StringRedisTemplate redisTemplate) {
		this.symbol = symbol;
		this.redisTemplate = redisTemplate;
	}

	@OnMessage
	public void onMessage(String message) {
		try {
			JsonNode node = objectMapper.readTree(message);
			JsonNode k = node.get("k");
			if (k == null) {
				LOGGER.info("[{}] 非 K 線訊息，略過。原始訊息前 80 字元：{}", symbol,
					message.length() > 80 ? message.substring(0, 80) : message);
				return;
			}

			String closePrice = k.get("c").asText();
			boolean isClosed = k.get("x").asBoolean();

			redisTemplate.opsForValue().set(
				CacheKeyConstants.latestPrice(symbol),
				closePrice,
				Duration.ofMinutes(2)
			);

			TRACE.message("[{}] 更新即時報價 → {} (isClosed={})", symbol, closePrice, isClosed);

			if (isClosed) {
				String closedKey = CacheKeyConstants.closedPriceList(symbol);
				redisTemplate.opsForList().leftPush(closedKey, closePrice);
				redisTemplate.opsForList().trim(closedKey, 0, 59);
				LOGGER.info("[{}] K 線收盤，推入 closedList，收盤價：{}", symbol, closePrice);
			}
		} catch (Exception e) {
			LOGGER.warn("[{}] 訊息解析失敗，等待下一條訊息。原因：{}", symbol, e.getMessage());
		}
	}

	@OnError
	public void onError(Throwable t) {
		LOGGER.warn("[{}] WebSocket 發生錯誤，等待 BinanceStreamAdapter重連。原因：{}", symbol,
			t.getMessage());
	}
}

package com.hcy.quant_core.modules.marketdata.adapter.stream;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.marketdata.port.IBinanceStreamPort;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
public class BinanceStreamAdapter implements IBinanceStreamPort {
	private static final Logger LOGGER = LoggerFactory.getLogger(BinanceStreamAdapter.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final ExecutorService virtualThreadExecutor;
	private final StringRedisTemplate redisTemplate;

	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	public BinanceStreamAdapter(
		@Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor,
		StringRedisTemplate redisTemplate) {
		this.virtualThreadExecutor = virtualThreadExecutor;
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void subscribe(Set<String> symbols) {
		for (String symbol : symbols) {
			virtualThreadExecutor.submit(() -> connectAndStream(symbol.toLowerCase()));
		}
	}

	private void connectAndStream(String symbol) {
		String wsUrl = "wss://stream.binance.com:9443/ws/" + symbol + "@kline_1m";
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			Session session = container.connectToServer(
				new BinanceWebSocketClient(symbol, redisTemplate),
				URI.create(wsUrl)
			);
			sessions.put(symbol, session);
			LOGGER.info("[{}] WebSocket connect success", symbol);
		} catch (Exception e) {
			try {
				Thread.sleep(5000);
				connectAndStream(symbol);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void unsubscribe(String symbol) {
		Session session = sessions.remove(symbol.toLowerCase());
		if (session != null && session.isOpen()) {
			try {
				session.close();
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public void stop() {
		sessions.values().forEach(s -> {
			try {
				s.close();
			} catch (Exception ignored) {
			}
		});
		sessions.clear();
	}
}

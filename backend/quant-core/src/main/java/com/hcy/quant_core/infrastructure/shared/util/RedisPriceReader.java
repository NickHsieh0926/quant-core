package com.hcy.quant_core.infrastructure.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RedisPriceReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisPriceReader.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final StringRedisTemplate redisTemplate;

	public RedisPriceReader(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public BigDecimal readPriceFromRedis(String key) {
		String val = redisTemplate.opsForValue().get(key);
		if (val == null)
			return null;
		try {
			return new BigDecimal(val);
		} catch (NumberFormatException e) {
			LOGGER.warn("Invalid price in Redis key={}: {}", key, val);
			return null;
		}
	}
}

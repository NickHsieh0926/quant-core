package com.hcy.quant_core.modules.marketdata.config;

import com.hcy.quant_core.modules.marketdata.port.IBinanceStreamPort;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

@Configuration
public class MarketDataStartupConfig {

	@Bean
	public ApplicationRunner startStreaming(
		IBinanceStreamPort streamPort,
		MarketDataProperties marketDataProperties) {
		return args -> {
			// application.properties 的 market.data.symbols 注入到 MarketDataProperties
			var symbols = new HashSet<>(marketDataProperties.getSymbols());
			streamPort.subscribe(symbols);
		};
	}
}

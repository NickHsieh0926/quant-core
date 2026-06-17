package com.hcy.quant_core.infrastructure.config;

import com.hcy.quant_core.modules.marketdata.config.MarketDataProperties;
import com.hcy.quant_core.modules.onchain.config.OnChainProperties;
import com.hcy.quant_core.modules.statarb.config.StatArbProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
	MarketDataProperties.class,
	StatArbProperties.class,
	OnChainProperties.class,
})
public class PropertiesConfig {}

package com.hcy.quant_core.modules.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "market.data")
public record MarketDataProperties(List<String> symbols) {}

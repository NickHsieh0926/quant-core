package com.hcy.quant_core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();  // HTTP 客戶端，全域共用
	}
}

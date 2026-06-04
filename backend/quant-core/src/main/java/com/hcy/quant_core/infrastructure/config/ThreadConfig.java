package com.hcy.quant_core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {

	@Bean(name = "virtualThreadExecutor")
	public ExecutorService virtualThreadExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
		scheduler.setVirtualThreads(true);
		scheduler.setThreadNamePrefix("quant-scheduler-");
		return scheduler;
	}
}

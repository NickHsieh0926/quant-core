package com.hcy.quant_core.modules.marketdata.adapter.stream;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class VirtualThreadStressTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(VirtualThreadStressTest.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private static final int THREAD_COUNT = 100;

	@Test
	void virtualThreads_shouldCompleteAllTasksWithinTimeLimit() throws InterruptedException {
		AtomicInteger successCount = new AtomicInteger(0);
		Instant start = Instant.now();

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < THREAD_COUNT; i++) {
				executor.submit(() -> {
					try {
						Thread.sleep(200);  // 模擬「任何 I/O 等待期間，Thread 被阻塞的狀態」
						successCount.incrementAndGet();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
			}
		}

		Duration elapsed = Duration.between(start, Instant.now());

		LOGGER.info("elapsed.toMillis:{}", elapsed.toMillis());
		
		assertThat(elapsed.toMillis())
			.as("Virtual Threads 應讓 100 個 I/O 任務近乎並發完成")
			.isLessThan(2000L);

		assertThat(successCount.get())
			.as("所有 Virtual Threads 應成功完成，無 deadlock")
			.isEqualTo(THREAD_COUNT);
	}

	// 量化對比，輸出加速倍數
	@Test
	void virtualThreads_shouldOutperformPlatformThreadPool() throws InterruptedException {
		long platformElapsed = measureElapsed(Executors.newFixedThreadPool(10));
		long virtualElapsed = measureElapsed(Executors.newVirtualThreadPerTaskExecutor());

		assertThat(virtualElapsed)
			.as("Virtual Threads 應比 Platform FixedThreadPool(10) 更快完成 I/O 任務")
			.isLessThan(platformElapsed);

		LOGGER.info("\nPlatform FixedThreadPool(10): {}ms\nVirtual Threads: {}ms\nSpeedup: {}x",
			platformElapsed,
			virtualElapsed,
			String.format("%.1f", (double) platformElapsed / virtualElapsed)
		);
	}

	private long measureElapsed(ExecutorService executor) throws InterruptedException {
		Instant start = Instant.now();
		try (executor) {
			for (int i = 0; i < THREAD_COUNT; i++) {
				int finalI = i;
				executor.submit(() -> {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
			}
		}
		return Duration.between(start, Instant.now()).toMillis();
	}
}

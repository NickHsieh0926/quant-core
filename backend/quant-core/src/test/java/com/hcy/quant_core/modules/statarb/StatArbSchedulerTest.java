package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import com.hcy.quant_core.modules.statarb.config.StatArbProperties;
import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.model.direction.StatArbDirection;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatArbSchedulerTest {

	@Mock
	private IStatArbUseCase statArbUseCase;
	@Mock
	private StatArbSignalPersistencePort persistencePort;
	@Mock
	private IAlertPublisher alertPublisher;
	@Mock
	private SignalAlertPersistencePort signalAlertPersistencePort;


	private final StatArbProperties props = new StatArbProperties(
		List.of(new StatArbProperties.PairConfig(
			"BTCUSDT", "ETHUSDT", 2.0, 0.5, 3.5, 30
		))
	);

	private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

	private StatArbScheduler scheduler;

	@BeforeEach
	void setUp() {
		// 每個 test new 一個新實例，確保 lastActiveDirection 從 HOLD 開始
		scheduler = new StatArbScheduler(statArbUseCase, persistencePort, alertPublisher, props,
			meterRegistry, signalAlertPersistencePort);
	}

	// symbol 固定 BTCUSDT/ETHUSDT
	private StatArbSignalRecord signalOf(String direction, double zScore) {
		return new StatArbSignalRecord(
			"BTCUSDT",
			"ETHUSDT",
			new BigDecimal("0"),
			new BigDecimal("0"),
			zScore,
			direction,
			LocalDateTime.now()
		);
	}

	private StatArbSignalRecord signalOf(String symbolA, String symbolB,
		String direction, double zScore) {
		return new StatArbSignalRecord(
			symbolA,
			symbolB,
			new BigDecimal("0"),
			new BigDecimal("0"),
			zScore,
			direction,
			LocalDateTime.now()
		);
	}


	//用指定 props 建立 Scheduler
	private StatArbScheduler schedulerWith(StatArbProperties p) {
		return new StatArbScheduler(
			statArbUseCase, persistencePort, alertPublisher, p, new SimpleMeterRegistry(),
			signalAlertPersistencePort
		);
	}

	// null signal
	@Test
	void whenSignalIsNull_doesNotPublish() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(null);

		scheduler.calculateAndPublish();

		verify(persistencePort, never()).save(any());
		verify(alertPublisher, never()).publish(any());
	}

	// 開倉：HOLD → OPEN_LONG_B
	@Test
	void holdToOpenLongB_publishesEntry() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));

		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.ENTRY);
		assertThat(captor.getValue().direction()).isEqualTo(StatArbDirection.OPEN_LONG_B);
	}

	// 開倉：HOLD → OPEN_SHORT_B
	@Test
	void holdToOpenShortB_publishesEntry() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_SHORT_B", -2.5));

		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.ENTRY);
		assertThat(captor.getValue().direction()).isEqualTo(StatArbDirection.OPEN_SHORT_B);
	}

	// 持倉不變：OPEN_LONG_B → OPEN_LONG_B，不重複推警報
	@Test
	void openLongB_stays_doesNotRepeatAlert() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));

		scheduler.calculateAndPublish();           // HOLD → OPEN_LONG_B（ENTRY）
		Mockito.clearInvocations(alertPublisher);  // 清零，只驗第 2 次

		scheduler.calculateAndPublish();           // OPEN_LONG_B → OPEN_LONG_B

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher, times(1)).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.NONE);
		assertThat(captor.getValue().direction()).isEqualTo(StatArbDirection.OPEN_LONG_B);
	}

	// 平倉：OPEN_LONG_B → HOLD，EXIT direction 必須是 OPEN_LONG_B（不是 HOLD）
	@Test
	void openLongB_toHold_publishesExitWithCorrectDirection() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));
		scheduler.calculateAndPublish();           // 建立多頭持倉
		Mockito.clearInvocations(alertPublisher);

		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("CLOSE", 0.3));
		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.EXIT);
		assertThat(captor.getValue().direction()).isEqualTo(StatArbDirection.OPEN_LONG_B);
	}

	// 平倉：OPEN_SHORT_B → HOLD，EXIT direction 必須是 OPEN_SHORT_B
	@Test
	void openShortB_toHold_publishesExitWithCorrectDirection() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_SHORT_B", -2.5));
		scheduler.calculateAndPublish();           // 建立空頭持倉
		Mockito.clearInvocations(alertPublisher);

		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("CLOSE", -0.3));
		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.EXIT);
		assertThat(captor.getValue().direction()).isEqualTo(StatArbDirection.OPEN_SHORT_B);
	}

	// 直翻：OPEN_LONG_B → OPEN_SHORT_B， EXIT(多頭) + ENTRY(空頭)
	@Test
	void openLongB_toOpenShortB_publishesExitThenEntry() {
		when(statArbUseCase.calculate(any(), any(),
			any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));
		scheduler.calculateAndPublish();           // 建立多頭持倉
		Mockito.clearInvocations(alertPublisher);

		when(statArbUseCase.calculate(any(), any(),
			any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_SHORT_B", -2.5));
		scheduler.calculateAndPublish();           // 直翻

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher, times(2)).publish(captor.capture());

		List<SignalAlertPayload> payloads = captor.getAllValues();
		assertThat(payloads.get(0).alertType()).isEqualTo(AlertType.EXIT);
		assertThat(payloads.get(0).direction()).isEqualTo(StatArbDirection.OPEN_LONG_B);

		assertThat(payloads.get(1).alertType()).isEqualTo(AlertType.ENTRY);
		assertThat(payloads.get(1).direction()).isEqualTo(StatArbDirection.OPEN_SHORT_B);
	}

	// 直翻：OPEN_SHORT_B → OPEN_LONG_B， EXIT(空頭) + ENTRY(多頭)
	@Test
	void openShortB_toOpenLongB_publishesExitThenEntry() {
		when(statArbUseCase.calculate(any(), any(),
			any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_SHORT_B", -2.5));
		scheduler.calculateAndPublish();           // 建立空頭持倉
		Mockito.clearInvocations(alertPublisher);

		when(statArbUseCase.calculate(any(), any(),
			any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));
		scheduler.calculateAndPublish();           // 直翻

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher, times(2)).publish(captor.capture());

		List<SignalAlertPayload> payloads = captor.getAllValues();
		assertThat(payloads.get(0).alertType()).isEqualTo(AlertType.EXIT);
		assertThat(payloads.get(0).direction()).isEqualTo(StatArbDirection.OPEN_SHORT_B);

		assertThat(payloads.get(1).alertType()).isEqualTo(AlertType.ENTRY);
		assertThat(payloads.get(1).direction()).isEqualTo(StatArbDirection.OPEN_LONG_B);
	}

	//  save 行為：signal 不為 null 時一律儲存
	@Test
	void whenSignalNotNull_alwaysSaves() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("HOLD", 1.0));

		scheduler.calculateAndPublish();

		verify(persistencePort).save(any(StatArbSignalRecord.class));
	}

	// 多個 pair 都被計算、save、publish
	@Test
	void multiplePairs_allSignalsSavedAndPublished() {
		StatArbProperties twoProps = new StatArbProperties(List.of(
			new StatArbProperties.PairConfig("BTCUSDT", "ETHUSDT", 2.0, 0.5, 3.5, 30),
			new StatArbProperties.PairConfig("SOLUSDT", "BTCUSDT", 2.0, 0.5, 3.5, 30)
		));
		StatArbScheduler twoPropsScheduler = schedulerWith(twoProps);

		when(statArbUseCase.calculate(eq("BTCUSDT"), eq("ETHUSDT"), any()))
			.thenReturn(signalOf("BTCUSDT", "ETHUSDT", "OPEN_LONG_B", 2.5));
		when(statArbUseCase.calculate(eq("SOLUSDT"), eq("BTCUSDT"), any()))
			.thenReturn(signalOf("SOLUSDT", "BTCUSDT", "HOLD", 1.0));

		twoPropsScheduler.calculateAndPublish();

		verify(persistencePort, times(2)).save(any());
		verify(alertPublisher, times(2)).publish(any());
	}

	// 某個 pair 拋出 Exception，不影響其他 pair
	@Test
	void whenOnePairThrowsException_otherPairStillProcessed() {
		StatArbProperties twoProps = new StatArbProperties(List.of(
			new StatArbProperties.PairConfig("BTCUSDT", "ETHUSDT", 2.0, 0.5, 3.5, 30),
			new StatArbProperties.PairConfig("SOLUSDT", "BTCUSDT", 2.0, 0.5, 3.5, 30)
		));
		StatArbScheduler twoPropsScheduler = schedulerWith(twoProps);

		when(statArbUseCase.calculate(eq("BTCUSDT"), eq("ETHUSDT"), any()))
			.thenThrow(new RuntimeException("[Test Error Msg]" +
				"OnePairThrowsException_otherPairStillProcessed"));
		when(statArbUseCase.calculate(eq("SOLUSDT"), eq("BTCUSDT"), any()))
			.thenReturn(signalOf("SOLUSDT", "BTCUSDT", "OPEN_LONG_B", 2.3));

		twoPropsScheduler.calculateAndPublish();

		verify(persistencePort, times(1)).save(any());
		verify(alertPublisher, times(1)).publish(any());

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.ENTRY);
		assertThat(captor.getValue().direction()).isEqualTo(StatArbDirection.OPEN_LONG_B);
	}

	// lastDirectionByPair 以 pairKey 分別追蹤，兩個 pair 的狀態互不影響
	@Test
	void multiplePairs_edgeTriggerStateIsIndependentPerPair() {
		StatArbProperties twoProps = new StatArbProperties(List.of(
			new StatArbProperties.PairConfig("BTCUSDT", "ETHUSDT", 2.0, 0.5, 3.5, 30),
			new StatArbProperties.PairConfig("SOLUSDT", "BTCUSDT", 2.0, 0.5, 3.5, 30)
		));
		StatArbScheduler twoScheduler = schedulerWith(twoProps);

		when(statArbUseCase.calculate(eq("BTCUSDT"), eq("ETHUSDT"), any()))
			.thenReturn(signalOf("BTCUSDT", "ETHUSDT", "OPEN_LONG_B", 2.5));
		when(statArbUseCase.calculate(eq("SOLUSDT"), eq("BTCUSDT"), any()))
			.thenReturn(signalOf("SOLUSDT", "BTCUSDT", "HOLD", 1.0));

		twoScheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher, times(2)).publish(captor.capture());

		List<SignalAlertPayload> payloads = captor.getAllValues();
		assertThat(payloads).anyMatch(p -> p.alertType() == AlertType.ENTRY);
		assertThat(payloads).anyMatch(p -> p.alertType() == AlertType.NONE);
	}

	// ReentrantLock 防重入測試
	@Test
	void whenSecondInvocationArrivesWhileFirstIsRunning_itIsSkipped()
		throws InterruptedException {

		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch firstCanFinish = new CountDownLatch(1);

		when(statArbUseCase.calculate(any(), any(), any())).thenAnswer(inv -> {
			firstStarted.countDown();   // Thread 1 已取得鎖並進入計算
			firstCanFinish.await();     // 模擬「執行中，鎖尚未釋放」
			return signalOf("HOLD", 0.5);
		});

		// Thread 1 模擬真實 cron 觸發
		Thread thread1 = Thread.ofVirtual().start(() -> scheduler.calculateAndPublish());
		firstStarted.await();           // 等 Thread 1 確實持有鎖後才繼續

		scheduler.calculateAndPublish(); // Thread 2：tryLock() 失敗 → skip

		// 釋放 Thread 1
		firstCanFinish.countDown();
		thread1.join(5_000);            // 最多等 5 秒，防止測試卡死

		verify(statArbUseCase, times(1)).calculate(any(), any(), any());
		verify(persistencePort, times(1)).save(any());
	}

	// skip 後 Counter 遞增、Gauge 遞增
	@Test
	void whenSkipped_counterIncrementedAndGaugeRises()
		throws InterruptedException {

		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch firstCanFinish = new CountDownLatch(1);

		when(statArbUseCase.calculate(any(), any(), any())).thenAnswer(inv -> {
			firstStarted.countDown();
			firstCanFinish.await();
			return signalOf("HOLD", 0.5);
		});

		Thread thread1 = Thread.ofVirtual().start(() -> scheduler.calculateAndPublish());
		firstStarted.await();

		scheduler.calculateAndPublish(); // Thread 2 skip → consecutiveSkips = 1

		firstCanFinish.countDown();      // 放行 Thread 1 → consecutiveSkips.set(0)
		thread1.join(5_000);

		assertThat(meterRegistry
			.get("statarb.scheduler.skip.total")
			.counter().count())
			.as("skip 1 次，Counter 應為 1")
			.isEqualTo(1.0);

		assertThat(meterRegistry
			.get("statarb.scheduler.consecutive.skips")
			.gauge().value())
			.as("Thread 2 的 +1 在 Thread 1 的 set(0) 之後，Gauge 應為 1")
			.isEqualTo(1.0);
	}

	// 成功執行後 Gauge 歸零,Counter 不歸零
	@Test
	void afterSuccessfulExecution_gaugeResetsToZero_butCounterKeepsTotal() {
		// 設初始狀態，模擬已 skip 兩次
		AtomicInteger consecutiveSkips =
			(AtomicInteger) ReflectionTestUtils.getField(scheduler, "consecutiveSkips");
		consecutiveSkips.set(2);

		// Pre-Assert — 確認 Gauge 確實為 2
		assertThat(meterRegistry
			.get("statarb.scheduler.consecutive.skips")
			.gauge().value()).isEqualTo(2.0);

		when(statArbUseCase.calculate(any(), any(), any()))
			.thenReturn(signalOf("HOLD", 0.5));
		scheduler.calculateAndPublish();

		assertThat(meterRegistry
			.get("statarb.scheduler.skip.total")
			.counter().count())
			.as("成功執行，Counter 不遞增")
			.isEqualTo(0.0);

		// Gauge：成功執行 → consecutiveSkips.set(0) 歸零
		assertThat(meterRegistry
			.get("statarb.scheduler.consecutive.skips")
			.gauge().value())
			.as("成功執行 → consecutiveSkips.set(0) ，Gauge 應為 0")
			.isEqualTo(0.0);
	}

	// Hold 時不推播 EXIT，signalAlertPersistencePort 不應被呼叫
	@Test
	void openLongB_toHold_doesNotPublishExitAndDoesNotSaveAlert() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));
		scheduler.calculateAndPublish();           // 建立多頭持倉
		Mockito.clearInvocations(alertPublisher, signalAlertPersistencePort);

		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("HOLD", 1.0));    // Z-Score 回到正常範圍，但尚未EXIT
		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(alertPublisher).publish(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.NONE);
		verify(signalAlertPersistencePort, never()).save(any());
	}

	// ENTRY 觸發時，signalAlertPersistencePort.save() 必須被呼叫一次
	@Test
	void onEntry_signalAlertIsSaved() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));

		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertRecord> captor =
			ArgumentCaptor.forClass(SignalAlertRecord.class);
		verify(signalAlertPersistencePort).save(captor.capture());

		assertThat(captor.getValue().strategy()).isEqualTo("STAT_ARB");
		assertThat(captor.getValue().direction()).isEqualTo("OPEN_LONG_B");
		assertThat(captor.getValue().alertType()).isEqualTo("ENTRY");
	}

	// NONE 時不寫入 signal_alert
	@Test
	void onNone_signalAlertIsNotSaved() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("HOLD", 0.5));

		scheduler.calculateAndPublish();

		verify(signalAlertPersistencePort, never()).save(any());
	}

	// EXIT 觸發時，signalAlertPersistencePort.save() 必須被呼叫一次，且 direction 是平倉前的持倉方向
	@Test
	void onExit_signalAlertIsSavedWithCorrectDirection() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));
		scheduler.calculateAndPublish();
		Mockito.clearInvocations(signalAlertPersistencePort);

		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("CLOSE", 0.2));
		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertRecord> captor =
			ArgumentCaptor.forClass(SignalAlertRecord.class);
		verify(signalAlertPersistencePort).save(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo("EXIT");
		// direction 應是平倉前的持倉方向 OPEN_LONG_B，不是 CLOSE
		assertThat(captor.getValue().direction()).isEqualTo("OPEN_LONG_B");
	}

	// 直翻時雙寫兩次（EXIT 舊方向 + ENTRY 新方向），且順序正確
	@Test
	void onDirectFlip_signalAlertSavedTwiceInOrder() {
		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_LONG_B", 2.5));
		scheduler.calculateAndPublish();
		Mockito.clearInvocations(signalAlertPersistencePort);

		when(statArbUseCase.calculate(any(), any(), any(StatArbParams.class)))
			.thenReturn(signalOf("OPEN_SHORT_B", -2.5));
		scheduler.calculateAndPublish();

		ArgumentCaptor<SignalAlertRecord> captor =
			ArgumentCaptor.forClass(SignalAlertRecord.class);
		verify(signalAlertPersistencePort, times(2)).save(captor.capture());

		List<SignalAlertRecord> saved = captor.getAllValues();
		assertThat(saved.get(0).alertType()).isEqualTo("EXIT");
		assertThat(saved.get(0).direction()).isEqualTo("OPEN_LONG_B");
		assertThat(saved.get(1).alertType()).isEqualTo("ENTRY");
		assertThat(saved.get(1).direction()).isEqualTo("OPEN_SHORT_B");
	}
}

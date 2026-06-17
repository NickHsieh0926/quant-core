package com.hcy.quant_core.modules.onchain;

import com.hcy.quant_core.modules.alert.model.SignalAlertRecord;
import com.hcy.quant_core.modules.alert.port.SignalAlertPersistencePort;
import com.hcy.quant_core.modules.onchain.model.OnChainSignalRecord;
import com.hcy.quant_core.modules.onchain.port.IOnChainUseCase;
import com.hcy.quant_core.modules.onchain.port.OnChainSignalPersistencePort;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.model.direction.OnChainDirection;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnChainSchedulerTest {

	@Mock
	private IOnChainUseCase mockUseCase;
	@Mock
	private IAlertPublisher mockPublisher;
	@Mock
	private JobLauncher mockLauncher;
	@Mock
	private Job mockJob;
	@Mock
	private OnChainSignalPersistencePort persistencePort;
	@Mock
	private SignalAlertPersistencePort signalAlertPersistencePort;

	private OnChainScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new OnChainScheduler(mockUseCase, mockPublisher, mockLauncher, mockJob,
			persistencePort, signalAlertPersistencePort);
	}

	// triggered=false，direction=NEUTRAL，score=50
	private OnChainSignalRecord neutralSignal() {
		return new OnChainSignalRecord(
			LocalDateTime.now(), 50, "Neutral", 50, "NEUTRAL", false, "RULE_BASED", null, "test " +
			"summary"
		);
	}

	// triggered=true，direction=BULLISH，score=80（> 70）
	private OnChainSignalRecord bullishSignal() {
		return new OnChainSignalRecord(
			LocalDateTime.now(), 20, "Extreme Fear", 80, "BULLISH", true, "RULE_BASED",
			new BigDecimal("65000"), "test summary"
		);
	}

	// triggered=true，direction=BEARISH，score=20（< 30）
	private OnChainSignalRecord bearishSignal() {
		return new OnChainSignalRecord(
			LocalDateTime.now(), 80, "Extreme Greed", 20, "BEARISH", true, "RULE_BASED",
			new BigDecimal("98000"), "test summary"
		);
	}

	// NEUTRAL → NEUTRAL
	@Test
	void evaluateAndPublish_neutralToNeutral_publishesWithNoneAlertType() {
		when(mockUseCase.calculateSignal()).thenReturn(neutralSignal());

		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		verify(mockPublisher, times(1)).publish(any());

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(mockPublisher).publish(captor.capture());
		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.NONE);
	}

	// NEUTRAL → BULLISH
	@Test
	void evaluateAndPublish_neutralToBullish_publishesEntry() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());

		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(mockPublisher).publish(captor.capture());
		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.ENTRY);
		assertThat(captor.getValue().direction()).isEqualTo(OnChainDirection.BULLISH);
	}

	// BULLISH → BULLISH（同極端區不重複推播）
	@Test
	void evaluateAndPublish_bullishToBullish_doesNotRepeatAlert() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());

		// 第 1 次：NEUTRAL → BULLISH，產生 ENTRY
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));
		Mockito.clearInvocations(mockPublisher);  // 清零，只驗第 2 次

		// 第 2 次：BULLISH → BULLISH，同方向不重推
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(mockPublisher, times(1)).publish(captor.capture());
		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.NONE);
		assertThat(captor.getValue().direction()).isEqualTo(OnChainDirection.BULLISH);
	}

	// BULLISH → NEUTRAL（離開極端區，推 EXIT）
	@Test
	void evaluateAndPublish_bullishToNeutral_publishesExit() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));
		Mockito.clearInvocations(mockPublisher);

		// score 回到中性，推 EXIT
		when(mockUseCase.calculateSignal()).thenReturn(neutralSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(mockPublisher, times(1)).publish(captor.capture());
		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.EXIT);
		assertThat(captor.getValue().direction()).isEqualTo(OnChainDirection.BULLISH);
	}

	// BEARISH → NEUTRAL
	@Test
	void evaluateAndPublish_bearishToNeutral_publishesExit() {
		when(mockUseCase.calculateSignal()).thenReturn(bearishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));
		Mockito.clearInvocations(mockPublisher);

		when(mockUseCase.calculateSignal()).thenReturn(neutralSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(mockPublisher, times(1)).publish(captor.capture());
		assertThat(captor.getValue().alertType()).isEqualTo(AlertType.EXIT);
		assertThat(captor.getValue().direction()).isEqualTo(OnChainDirection.BEARISH);
	}

	// BULLISH → BEARISH（直翻，推 EXIT + ENTRY 兩個 payload）
	@Test
	void evaluateAndPublish_bullishToBearish_sendsExitThenEntry() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));
		Mockito.clearInvocations(mockPublisher);

		// Step 2：直翻 → BEARISH
		when(mockUseCase.calculateSignal()).thenReturn(bearishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		// 第 2 次呼叫必須產生 2 個 publish（EXIT + ENTRY）
		ArgumentCaptor<SignalAlertPayload> captor =
			ArgumentCaptor.forClass(SignalAlertPayload.class);
		verify(mockPublisher, times(2)).publish(captor.capture());

		List<SignalAlertPayload> payloads = captor.getAllValues();

		// 第 1 個 payload 是 EXIT（通知舊 BULLISH 倉位出場）
		assertThat(payloads.get(0).alertType())
			.as("直翻時第一個 payload 應為 EXIT（通知舊方向倉位出場）")
			.isEqualTo(AlertType.EXIT);
		assertThat(payloads.get(0).direction()).isEqualTo(OnChainDirection.BULLISH);

		// 第 2 個 payload 是 ENTRY（開啟新 BEARISH 方向）
		assertThat(payloads.get(1).alertType())
			.as("直翻時第二個 payload 應為 ENTRY（新方向進場）")
			.isEqualTo(AlertType.ENTRY);
		assertThat(payloads.get(1).direction()).isEqualTo(OnChainDirection.BEARISH);
	}

	// calculateSignal() 回傳 null（資料庫無數據）
	@Test
	void evaluateAndPublish_whenSignalIsNull_doesNotPublish() {
		when(mockUseCase.calculateSignal()).thenReturn(null);

		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		verify(mockPublisher, never()).publish(any());
	}

	// on_chain_signal 每次有 signal 都持久化
	@Test
	void evaluateAndPublish_alwaysSavesOnChainSignal() {
		when(mockUseCase.calculateSignal()).thenReturn(neutralSignal());

		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		verify(persistencePort).save(any(OnChainSignalRecord.class));
	}

	// signal = null 時，on_chain_signal 不持久化
	@Test
	void evaluateAndPublish_whenNull_doesNotSaveOnChainSignal() {
		when(mockUseCase.calculateSignal()).thenReturn(null);

		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		verify(persistencePort, never()).save(any());
	}

	// ENTRY 時 signal_alert 寫入一筆，且 symbolB = null（單邊策略）
	@Test
	void onEntry_signalAlertIsSavedWithNullSymbolB() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());

		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		ArgumentCaptor<SignalAlertRecord> captor =
			ArgumentCaptor.forClass(SignalAlertRecord.class);
		verify(signalAlertPersistencePort).save(captor.capture());

		assertThat(captor.getValue().alertType()).isEqualTo("ENTRY");
		assertThat(captor.getValue().strategy()).isEqualTo("ON_CHAIN");
		assertThat(captor.getValue().symbolB()).isNull();   // 單邊策略無 symbolB
	}

	// NONE 時 signal_alert 不寫入（BULLISH → BULLISH 同方向不重推）
	@Test
	void onNone_signalAlertIsNotSaved() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));
		Mockito.clearInvocations(signalAlertPersistencePort);

		// BULLISH → BULLISH：AlertType.NONE
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		verify(signalAlertPersistencePort, never()).save(any());
	}

	// 直翻時 signal_alert 寫兩次（EXIT + ENTRY），順序正確
	@Test
	void onDirectFlip_signalAlertSavedTwiceInOrder() {
		when(mockUseCase.calculateSignal()).thenReturn(bullishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));
		Mockito.clearInvocations(signalAlertPersistencePort);

		when(mockUseCase.calculateSignal()).thenReturn(bearishSignal());
		scheduler.evaluateAndPublish(new OnChainJobCompletedEvent(this));

		ArgumentCaptor<SignalAlertRecord> captor =
			ArgumentCaptor.forClass(SignalAlertRecord.class);
		verify(signalAlertPersistencePort, times(2)).save(captor.capture());

		List<SignalAlertRecord> saved = captor.getAllValues();
		assertThat(saved.get(0).alertType()).isEqualTo("EXIT");
		assertThat(saved.get(0).direction()).isEqualTo("BULLISH");
		assertThat(saved.get(1).alertType()).isEqualTo("ENTRY");
		assertThat(saved.get(1).direction()).isEqualTo("BEARISH");
	}
}

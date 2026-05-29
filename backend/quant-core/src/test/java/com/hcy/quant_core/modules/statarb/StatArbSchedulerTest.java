package com.hcy.quant_core.modules.statarb;

import com.hcy.quant_core.modules.statarb.model.StatArbParams;
import com.hcy.quant_core.modules.statarb.model.StatArbSignalRecord;
import com.hcy.quant_core.modules.statarb.port.IStatArbUseCase;
import com.hcy.quant_core.modules.statarb.port.StatArbSignalPersistencePort;
import com.hcy.quant_core.modules.websocket.model.AlertType;
import com.hcy.quant_core.modules.websocket.model.SignalAlertPayload;
import com.hcy.quant_core.modules.websocket.model.direction.StatArbDirection;
import com.hcy.quant_core.modules.websocket.port.IAlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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

	private StatArbScheduler scheduler;

	@BeforeEach
	void setUp() {
		// 每個 test new 一個新實例，確保 lastActiveDirection 從 HOLD 開始
		scheduler = new StatArbScheduler(statArbUseCase, persistencePort, alertPublisher);
	}

	private StatArbSignalRecord signalOf(String direction, double zScore) {
		boolean triggered = direction.equals("OPEN_LONG_B") || direction.equals("OPEN_SHORT_B");
		return new StatArbSignalRecord(
			"BTCUSDT",
			"ETHUSDT",
			zScore,
			direction,
			triggered,
			LocalDateTime.now()
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
			.thenReturn(signalOf("HOLD", 0.3));
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
			.thenReturn(signalOf("HOLD", -0.3));
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
}

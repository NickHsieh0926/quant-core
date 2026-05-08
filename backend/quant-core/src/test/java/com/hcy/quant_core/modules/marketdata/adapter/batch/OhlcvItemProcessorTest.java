package com.hcy.quant_core.modules.marketdata.adapter.batch;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;
import com.hcy.quant_core.modules.marketdata.port.OhlcvPersistencePort;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OhlcvItemProcessorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(OhlcvItemProcessorTest.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final OhlcvPersistencePort mockPort = mock(OhlcvPersistencePort.class);
	private final OhlcvItemProcessor processor = new OhlcvItemProcessor(mockPort);

	private OhlcvRecord sampleRecord() {
		return new OhlcvRecord("BTCUSDT", 1672531200000L,
			new BigDecimal("16500.00"), new BigDecimal("16600.00"),
			new BigDecimal("16400.00"), new BigDecimal("16550.00"),
			new BigDecimal("1234.56"), "1d");
	}

	@Test
	void process_whenRecordExists_returnsNull() throws Exception {
		OhlcvRecord record = sampleRecord();
		when(mockPort.existsBySymbolAndOpenTimeAndInterval(
			record.symbol(), record.openTime(), record.interval()))
			.thenReturn(true);

		assertThat(processor.process(record)).isNull();
	}

	@Test
	void process_whenRecordNotExists_returnsRecord() throws Exception {
		OhlcvRecord record = sampleRecord();
		when(mockPort.existsBySymbolAndOpenTimeAndInterval(
			record.symbol(), record.openTime(), record.interval()))
			.thenReturn(false);

		assertThat(processor.process(record)).isEqualTo(record);
	}
}

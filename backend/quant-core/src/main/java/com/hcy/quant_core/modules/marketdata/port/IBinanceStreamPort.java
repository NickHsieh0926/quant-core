package com.hcy.quant_core.modules.marketdata.port;

import java.util.Set;

public interface IBinanceStreamPort {
	void subscribe(Set<String> symbols);

	void unsubscribe(String symbol);

	void stop();
}

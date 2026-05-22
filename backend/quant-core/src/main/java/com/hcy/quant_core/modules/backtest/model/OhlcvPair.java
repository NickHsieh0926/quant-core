package com.hcy.quant_core.modules.backtest.model;

import com.hcy.quant_core.modules.marketdata.model.OhlcvRecord;

public record OhlcvPair(
	OhlcvRecord recordA,
	OhlcvRecord recordB
) {}

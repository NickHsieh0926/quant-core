package com.hcy.quant_core.modules.backtest.adapter.batch;

import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.modules.backtest.model.OhlcvPair;
import com.hcy.quant_core.modules.backtest.model.TradeResult;
import com.hcy.quant_core.modules.statarb.calculator.ZScoreCalculator;
import com.hcy.quant_core.modules.statarb.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BacktestItemProcessor implements ItemProcessor<OhlcvPair, TradeResult> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BacktestItemProcessor.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	private final Strategy strategy;
	private final ZScoreCalculator zScoreCalculator;

	// 滾動視窗（@StepScope 保證每次 Job 重置）
	private final List<BigDecimal> priceBufferA = new ArrayList<>();
	private final List<BigDecimal> priceBufferB = new ArrayList<>();

	public BacktestItemProcessor(Strategy strategy, ZScoreCalculator zScoreCalculator) {
		this.strategy = strategy;
		this.zScoreCalculator = zScoreCalculator;
	}

	@Override
	public TradeResult process(OhlcvPair pair) {
		return switch (strategy) {
			case MeanReversionStrategy s -> processMeanReversion(s, pair);
			case OnChainSignalStrategy s -> null;
			case MaCrossStrategy s -> null;
			case TechnicalAnalysisStrategy s -> null;
		};
	}

	private TradeResult processMeanReversion(MeanReversionStrategy s, OhlcvPair pair) {
		priceBufferA.add(pair.recordA().close());
		priceBufferB.add(pair.recordB().close());

		TRACE.message("priceBufferA.size()={}, s.lookBackDays()={}", priceBufferA.size(),
			s.lookBackDays());
		// 累積數據，不做操作
		if (priceBufferA.size() < s.lookBackDays())
			return null;

		// 滾動視窗
		if (priceBufferA.size() > s.lookBackDays()) {
			priceBufferA.removeFirst();
			priceBufferB.removeFirst();
		}

		double zScore = zScoreCalculator.calculate(priceBufferA, priceBufferB);

		String action;
		if (zScore > 3.0) {
			action = "STOP_LOSS";     // 價差擴大，策略失效預警
		} else if (zScore > s.entryZScore()) {
			action = "OPEN_LONG_B";   // symbolB 相對 symbolA 被低估，做多 symbolB
		} else if (zScore < s.exitZScore()) {
			action = "CLOSE";         // 價差回歸，平倉
		} else {
			action = "HOLD";
		}

		TRACE.message("zScore={}, action = {}", zScore, action);

		// HOLD 持倉，不做操作
		if ("HOLD".equals(action))
			return null;

		return new TradeResult(
			pair.recordA().openTime(),
			action,
			zScore,
			pair.recordA().close(),
			pair.recordB().close()
		);
	}
}

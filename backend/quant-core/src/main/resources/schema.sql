-- =============================================
-- Spring Batch Meta Tables
-- =============================================
-- Job 的基本資料（名稱、參數)
CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

COMMENT ON TABLE BATCH_JOB_INSTANCE IS 'Spring Batch Job 的基本資料（名稱、參數）';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.VERSION         IS '樂觀鎖版本號';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.JOB_NAME        IS 'Job 名稱';
COMMENT ON COLUMN BATCH_JOB_INSTANCE.JOB_KEY         IS 'Job 參數的 MD5 雜湊';

-- Job 每次執行的狀態（成功 / 失敗 / 執行中）
CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION          BIGINT,
    JOB_INSTANCE_ID  BIGINT NOT NULL,
    CREATE_TIME      TIMESTAMP    NOT NULL,
    START_TIME       TIMESTAMP DEFAULT NULL,
    END_TIME         TIMESTAMP DEFAULT NULL,
    STATUS           VARCHAR(10),
    EXIT_CODE        VARCHAR(2500),
    EXIT_MESSAGE     VARCHAR(2500),
    LAST_UPDATED     TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES
    BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

COMMENT ON TABLE BATCH_JOB_EXECUTION IS 'Job 每次執行的狀態（成功 / 失敗 / 執行中）';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.VERSION          IS '樂觀鎖版本號';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.JOB_INSTANCE_ID  IS '所屬JobInstance';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.CREATE_TIME      IS '建立時間';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.START_TIME       IS '執行開始時間';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.END_TIME         IS '執行結束時間';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.STATUS           IS '執行狀態（COMPLETED / FAILED / STARTED / STOPPED）';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.EXIT_CODE        IS '結束代碼';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.EXIT_MESSAGE     IS '結束訊息，失敗時記錄 StackTrace';
COMMENT ON COLUMN BATCH_JOB_EXECUTION.LAST_UPDATED     IS '最後更新時間';

-- Job 執行時帶的參數
CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    PARAMETER_NAME   VARCHAR(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1)      NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES
    BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

COMMENT ON TABLE BATCH_JOB_EXECUTION_PARAMS IS 'Job 執行時帶的參數';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.PARAMETER_NAME   IS '參數名稱';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.PARAMETER_TYPE   IS '參數型別';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.PARAMETER_VALUE  IS '參數值';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_PARAMS.IDENTIFYING      IS '是否為識別參數';

-- 每個 Step 的狀態與統計（讀幾筆、寫幾筆）
CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    CREATE_TIME        TIMESTAMP    NOT NULL,
    START_TIME         TIMESTAMP DEFAULT NULL,
    END_TIME           TIMESTAMP DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES
    BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

COMMENT ON TABLE BATCH_STEP_EXECUTION IS 'Job 內每個 Step 的執行狀態與讀寫統計';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.VERSION            IS '樂觀鎖版本號';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.STEP_NAME          IS 'Step 名稱';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.JOB_EXECUTION_ID   IS '所屬 Job 執行記錄';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.CREATE_TIME        IS '建立時間';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.START_TIME         IS '開始時間';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.END_TIME           IS '結束時間';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.STATUS             IS '執行狀態';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.COMMIT_COUNT       IS '已提交的 Chunk 次數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.READ_COUNT         IS 'ItemReader 讀取總筆數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.FILTER_COUNT       IS 'ItemProcessor 回傳 null 被過濾的筆數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.WRITE_COUNT        IS 'ItemWriter 寫入總筆數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.READ_SKIP_COUNT    IS 'Reader 階段 Skip 筆數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.WRITE_SKIP_COUNT   IS 'Writer 階段 Skip 筆數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.PROCESS_SKIP_COUNT IS 'Processor 階段 Skip 筆數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.ROLLBACK_COUNT     IS 'Chunk 回滾次數';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.EXIT_CODE          IS '結束代碼';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.EXIT_MESSAGE       IS '結束訊息';
COMMENT ON COLUMN BATCH_STEP_EXECUTION.LAST_UPDATED       IS '最後更新時間';

-- Step 中斷時的快照（用於 Restart）
CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES
    BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

COMMENT ON TABLE BATCH_STEP_EXECUTION_CONTEXT IS 'Step 中斷時的快照（用於 Restart）';
COMMENT ON COLUMN BATCH_STEP_EXECUTION_CONTEXT.STEP_EXECUTION_ID  IS '所屬 Step 執行記錄';
COMMENT ON COLUMN BATCH_STEP_EXECUTION_CONTEXT.SHORT_CONTEXT      IS '精簡上下文（VARCHAR）';
COMMENT ON COLUMN BATCH_STEP_EXECUTION_CONTEXT.SERIALIZED_CONTEXT IS '完整序列化上下文（TEXT），複雜狀態用此欄位';

-- Job 層級的中斷快照
CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES
    BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

COMMENT ON TABLE BATCH_JOB_EXECUTION_CONTEXT IS 'Job 層級的中斷快照';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_CONTEXT.JOB_EXECUTION_ID   IS '所屬 Job 執行記錄';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_CONTEXT.SHORT_CONTEXT      IS '精簡上下文（VARCHAR）';
COMMENT ON COLUMN BATCH_JOB_EXECUTION_CONTEXT.SERIALIZED_CONTEXT IS '完整序列化上下文（TEXT）';

CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ  MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ            MAXVALUE 9223372036854775807 NO CYCLE;


-- =============================================
-- Business Tables
-- =============================================
-- OHLCV 歷史 K 線（統計套利軌核心數據）
CREATE TABLE IF NOT EXISTS ohlcv (
    id          BIGSERIAL PRIMARY KEY,
    symbol      VARCHAR(20)    NOT NULL,
    open_time   BIGINT         NOT NULL,
    open        DECIMAL(20, 8) NOT NULL,
    high        DECIMAL(20, 8) NOT NULL,
    low         DECIMAL(20, 8) NOT NULL,
    close       DECIMAL(20, 8) NOT NULL,
    volume      DECIMAL(20, 8) NOT NULL,
    interval    VARCHAR(10)    NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (symbol, open_time, interval)
);

COMMENT ON TABLE ohlcv IS 'Binance OHLCV 歷史 K 線，';
COMMENT ON COLUMN ohlcv.symbol     IS '交易標的';
COMMENT ON COLUMN ohlcv.open_time  IS 'K 線開始時間（Unix 毫秒）';
COMMENT ON COLUMN ohlcv.open       IS '開盤價';
COMMENT ON COLUMN ohlcv.high       IS '最高價';
COMMENT ON COLUMN ohlcv.low        IS '最低價';
COMMENT ON COLUMN ohlcv.close      IS '收盤價';
COMMENT ON COLUMN ohlcv.volume     IS '成交量';
COMMENT ON COLUMN ohlcv.interval   IS 'K 線週期';

-- 回測績效結果（每次回測 Job 的彙總）
CREATE TABLE IF NOT EXISTS backtest_result (
    id              BIGSERIAL PRIMARY KEY,
    job_id          VARCHAR(50)    NOT NULL UNIQUE,
    strategy        VARCHAR(50)    NOT NULL,
    symbol_a        VARCHAR(20)    NOT NULL,
    symbol_b        VARCHAR(20)    NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    sharpe_ratio    DECIMAL(10, 4),
    max_drawdown    DECIMAL(10, 4),
    win_rate        DECIMAL(10, 4),
    annual_return   DECIMAL(10, 4),
    total_trades    INT,
    created_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE backtest_result IS '回測 Job 彙總績效';
COMMENT ON COLUMN backtest_result.job_id        IS 'Spring Batch JobExecution ID';
COMMENT ON COLUMN backtest_result.strategy      IS '策略名稱';
COMMENT ON COLUMN backtest_result.symbol_a      IS '交易標的 A';
COMMENT ON COLUMN backtest_result.symbol_b      IS '交易標的 B';
COMMENT ON COLUMN backtest_result.start_date    IS '回測起始日期，第一筆有效信號（非 HOLD）的日期';
COMMENT ON COLUMN backtest_result.end_date      IS '回測結束日期，最後一筆有效信號的日期';
COMMENT ON COLUMN backtest_result.sharpe_ratio  IS '夏普值（風險調整後報酬），>1.0 為合格，>2.0 為優秀';
COMMENT ON COLUMN backtest_result.max_drawdown  IS '最大回撤（0-1），越小越好，>0.15 為警戒線';
COMMENT ON COLUMN backtest_result.win_rate      IS '勝率（0-1），需搭配 Sharpe Ratio 一起看，高勝率低賠率仍可能虧損';
COMMENT ON COLUMN backtest_result.annual_return IS '年化報酬率（0-1）';
COMMENT ON COLUMN backtest_result.total_trades  IS '總交易筆數，過少（<30）代表樣本不足，績效結論不可信';

-- 鏈上指標時序（鏈上信號軌核心數據）
CREATE TABLE IF NOT EXISTS on_chain_metrics (
    id                  BIGSERIAL PRIMARY KEY,
    recorded_at         TIMESTAMP      NOT NULL,
    fear_greed_index    INT,
    fear_greed_label    VARCHAR(20),
    btc_exchange_flow   DECIMAL(20, 2),
    nupl                DECIMAL(10, 4),
    sopr                DECIMAL(10, 4),
    created_at          TIMESTAMP DEFAULT NOW(),
    UNIQUE (recorded_at)
);

COMMENT ON TABLE on_chain_metrics IS '鏈上市場指標快照';
COMMENT ON COLUMN on_chain_metrics.recorded_at       IS '指標快照時間點（UTC）';
COMMENT ON COLUMN on_chain_metrics.fear_greed_index  IS '貪婪恐懼指數，<25 極度恐懼（歷史底部區），>80 極度貪婪（歷史高點區）';
COMMENT ON COLUMN on_chain_metrics.fear_greed_label  IS 'API 回傳文字分類：Extreme Fear / Fear / Neutral / Greed / Extreme Greed';
COMMENT ON COLUMN on_chain_metrics.btc_exchange_flow IS 'BTC 交易所淨流量(BTC)，負=流出（持幣意願強，偏多），正=流入（準備賣出，偏空）';
COMMENT ON COLUMN on_chain_metrics.nupl              IS '未實現盈虧比率，接近 0=市場整體成本接近現價，極高=市場過熱';
COMMENT ON COLUMN on_chain_metrics.sopr              IS '已花費輸出獲利比率，<1=虧損賣出（底部特徵），>>1=大量獲利了結';
COMMENT ON COLUMN on_chain_metrics.created_at        IS '入庫時間';

-- 統計套利分析紀錄
CREATE TABLE IF NOT EXISTS stat_arb_signal (
    id          BIGSERIAL PRIMARY KEY,
    symbol_a    VARCHAR(20)     NOT NULL,
    symbol_b    VARCHAR(20)     NOT NULL,
    z_score     DECIMAL(10, 4)  NOT NULL,
    direction   VARCHAR(20)     NOT NULL,
    signal_at   TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE stat_arb_signal IS '統計套利信號記錄';
COMMENT ON COLUMN stat_arb_signal.id         IS '主鍵';
COMMENT ON COLUMN stat_arb_signal.symbol_a   IS '交易對 A';
COMMENT ON COLUMN stat_arb_signal.symbol_b   IS '交易對 B';
COMMENT ON COLUMN stat_arb_signal.z_score    IS '觸發時的 Z-Score';
COMMENT ON COLUMN stat_arb_signal.direction  IS '信號方向（OPEN_LONG / OPEN_SHORT / CLOSE / HOLD）';
COMMENT ON COLUMN stat_arb_signal.signal_at  IS '信號產生時間';

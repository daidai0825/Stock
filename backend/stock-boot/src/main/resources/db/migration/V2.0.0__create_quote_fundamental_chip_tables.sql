-- ============================================================
-- V2.0.0  建立 Wave 2 業務資料表（PostgreSQL 16）
-- 包含：STOCK_QUOTE（行情）、STOCK_FUNDAMENTAL（基本面）、STOCK_CHIP（籌碼）
-- 命名規範：snake_case 小寫，避免保留字，主鍵 VARCHAR(36) UUID
-- 時間：TIMESTAMP WITHOUT TIME ZONE（應用層固定 GMT+8）
-- ============================================================

-- ============================================================
-- 1. STOCK_QUOTE 每日行情
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_quote (
    quote_id      VARCHAR(36)     NOT NULL,
    stock_id      VARCHAR(20)     NOT NULL,
    stock_name    VARCHAR(100),
    market        VARCHAR(10)     NOT NULL DEFAULT 'TWSE',
    open_price    NUMERIC(12, 2)  NOT NULL,
    high_price    NUMERIC(12, 2)  NOT NULL,
    low_price     NUMERIC(12, 2)  NOT NULL,
    close_price   NUMERIC(12, 2)  NOT NULL,
    volume        BIGINT          NOT NULL DEFAULT 0,
    quote_date    DATE            NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_stock_quote PRIMARY KEY (quote_id),
    CONSTRAINT uk_stock_quote_stock_date UNIQUE (stock_id, quote_date)
);

CREATE INDEX IF NOT EXISTS idx_stock_quote_stock_id_date
    ON stock_quote (stock_id, quote_date DESC);
CREATE INDEX IF NOT EXISTS idx_stock_quote_date
    ON stock_quote (quote_date DESC);

COMMENT ON TABLE  stock_quote IS '每日行情（M-QUOTE, Wave 2）';
COMMENT ON COLUMN stock_quote.quote_id    IS 'UUID 字串';
COMMENT ON COLUMN stock_quote.stock_id    IS '股票代號（TWSE/OTC）';
COMMENT ON COLUMN stock_quote.market      IS 'TWSE（上市） / OTC（上櫃）';
COMMENT ON COLUMN stock_quote.volume      IS '成交量（股）';
COMMENT ON COLUMN stock_quote.quote_date  IS '交易日期';

-- ============================================================
-- 2. STOCK_FUNDAMENTAL 基本面
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_fundamental (
    fundamental_id  VARCHAR(36)     NOT NULL,
    stock_id        VARCHAR(20)     NOT NULL,
    stock_name      VARCHAR(100),
    eps             NUMERIC(10, 2),
    per_ratio       NUMERIC(10, 2),
    pbr_ratio       NUMERIC(10, 2),
    roe             NUMERIC(10, 2),
    report_year     SMALLINT        NOT NULL,
    report_quarter  SMALLINT        NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_stock_fundamental PRIMARY KEY (fundamental_id),
    CONSTRAINT uk_stock_fundamental_stock_id UNIQUE (stock_id)
);

CREATE INDEX IF NOT EXISTS idx_stock_fundamental_stock_id
    ON stock_fundamental (stock_id);

COMMENT ON TABLE  stock_fundamental IS '股票基本面（M-FUND, Wave 2）';
COMMENT ON COLUMN stock_fundamental.eps          IS '近四季合計 EPS';
COMMENT ON COLUMN stock_fundamental.per_ratio    IS '本益比（P/E Ratio）';
COMMENT ON COLUMN stock_fundamental.pbr_ratio    IS '股價淨值比（P/B Ratio）';
COMMENT ON COLUMN stock_fundamental.roe          IS '股東權益報酬率（%）';
COMMENT ON COLUMN stock_fundamental.report_year  IS '最新報告年度（西元年）';
COMMENT ON COLUMN stock_fundamental.report_quarter IS '最新報告季度（1-4）';

-- ============================================================
-- 3. STOCK_CHIP 籌碼（三大法人）
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_chip (
    chip_id                       VARCHAR(36)     NOT NULL,
    stock_id                      VARCHAR(20)     NOT NULL,
    stock_name                    VARCHAR(100),
    market                        VARCHAR(10)     NOT NULL DEFAULT 'TWSE',
    trade_date                    DATE            NOT NULL,
    foreign_net_shares            NUMERIC(18, 0)  NOT NULL DEFAULT 0,
    investment_trust_net_shares   NUMERIC(18, 0)  NOT NULL DEFAULT 0,
    dealer_net_shares             NUMERIC(18, 0)  NOT NULL DEFAULT 0,
    total_institutional_net       NUMERIC(18, 0)  NOT NULL DEFAULT 0,
    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_stock_chip PRIMARY KEY (chip_id),
    CONSTRAINT uk_stock_chip_stock_date UNIQUE (stock_id, trade_date)
);

CREATE INDEX IF NOT EXISTS idx_stock_chip_stock_id_date
    ON stock_chip (stock_id, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_stock_chip_date
    ON stock_chip (trade_date DESC);

COMMENT ON TABLE  stock_chip IS '三大法人籌碼（M-CHIP, Wave 2）';
COMMENT ON COLUMN stock_chip.foreign_net_shares          IS '外資買賣超（股，正為買超）';
COMMENT ON COLUMN stock_chip.investment_trust_net_shares IS '投信買賣超（股）';
COMMENT ON COLUMN stock_chip.dealer_net_shares           IS '自營商買賣超（股）';
COMMENT ON COLUMN stock_chip.total_institutional_net     IS '三大法人合計買賣超（股）';

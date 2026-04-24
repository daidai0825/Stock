-- 測試用 stock_info 建表腳本（對應 V3.0.0 migration，但不包含 pg_trgm index）
-- pg_trgm extension 需 superuser 權限（決策 P5），測試環境不啟用

CREATE TABLE IF NOT EXISTS stock_info (
    stock_id        VARCHAR(20)  NOT NULL,
    stock_name      VARCHAR(100) NOT NULL,
    stock_name_en   VARCHAR(200),
    market          VARCHAR(10)  NOT NULL,
    industry        VARCHAR(50),
    listed_date     DATE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP,
    CONSTRAINT pk_stock_info PRIMARY KEY (stock_id),
    CONSTRAINT ck_stock_info_market CHECK (market IN ('TWSE', 'OTC'))
);

-- 代號前綴索引
CREATE INDEX IF NOT EXISTS idx_stock_info_id_prefix
    ON stock_info (stock_id text_pattern_ops);

-- 中文名稱前綴索引
CREATE INDEX IF NOT EXISTS idx_stock_info_name
    ON stock_info (stock_name text_pattern_ops);

-- 英文名稱小寫索引
CREATE INDEX IF NOT EXISTS idx_stock_info_name_en_lower
    ON stock_info (LOWER(stock_name_en) text_pattern_ops);

-- 市場 + 有效狀態索引
CREATE INDEX IF NOT EXISTS idx_stock_info_market_active
    ON stock_info (market, is_active);

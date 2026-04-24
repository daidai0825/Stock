-- ============================================================
-- V3.0.0  Wave 3：建立股票主檔（搜尋來源）
-- 對應模組：stock-search
-- 對應文件：docs/04_architecture/project/20260423_wave3_flyway-migration-plan.md §3
-- ============================================================
--
-- 注意（P5 決策）：pg_trgm extension 需 DBA 預先以 superuser 執行：
--   CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- 應用層 DB user 無 superuser 權限，Migration 不執行 CREATE EXTENSION。
-- dev / prod 環境請 DBA 確認 extension 已就位後再啟動應用。
-- ============================================================

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

-- 股票代號前綴搜尋（text_pattern_ops 支援 LIKE 'xxx%'）
CREATE INDEX IF NOT EXISTS idx_stock_info_id_prefix
    ON stock_info (stock_id text_pattern_ops);

-- 中文名稱前綴 / 完全匹配（一般 BTREE，LIKE '台積%'）
CREATE INDEX IF NOT EXISTS idx_stock_info_name
    ON stock_info (stock_name text_pattern_ops);

-- 中文名稱部分匹配 GIN pg_trgm（需 DBA 預先啟用 pg_trgm extension）
-- 若 DBA 未啟用 pg_trgm，本 index 建立會失敗 → 改走 stock_name LIKE '%keyword%'（效能較差但可運作）
CREATE INDEX IF NOT EXISTS idx_stock_info_name_trgm
    ON stock_info USING GIN (stock_name gin_trgm_ops);

-- 英文名稱大小寫不敏感搜尋（LOWER + text_pattern_ops）
CREATE INDEX IF NOT EXISTS idx_stock_info_name_en_lower
    ON stock_info (LOWER(stock_name_en) text_pattern_ops);

-- 市場別 + 有效狀態（快速過濾下市股）
CREATE INDEX IF NOT EXISTS idx_stock_info_market_active
    ON stock_info (market, is_active);

COMMENT ON TABLE  stock_info                IS 'Wave 3：股票主檔（搜尋來源，TWSE+OTC）';
COMMENT ON COLUMN stock_info.stock_id       IS '股票代號（例：2330）；VARCHAR(20) 對應業務 ID，非自增 PK';
COMMENT ON COLUMN stock_info.stock_name     IS '中文名稱（例：台積電）';
COMMENT ON COLUMN stock_info.stock_name_en  IS '英文名稱（例：TSMC），可 NULL';
COMMENT ON COLUMN stock_info.market         IS '市場別：TWSE（上市）/ OTC（上櫃）';
COMMENT ON COLUMN stock_info.listed_date    IS '上市 / 上櫃日期，可 NULL';
COMMENT ON COLUMN stock_info.is_active      IS '是否仍在交易（下市 / 下櫃為 false）';
COMMENT ON COLUMN stock_info.created_at     IS '建立時間（UTC，應用層渲染時轉 GMT+8）';
COMMENT ON COLUMN stock_info.updated_at     IS '最後更新時間（StockInfoSyncService 每日 02:00 GMT+8 更新）';

-- ============================================================
-- V3.0.1  Wave 3：股票主檔初始 seed（TWSE + OTC 樣本）
-- 完整資料由 stock-search.StockInfoSyncService 每日 02:00 GMT+8 從 TWSE / OTC 同步
-- 此 seed 僅供 local / dev 啟動驗證用，prod 不依賴 seed 資料
-- ============================================================

INSERT INTO stock_info (stock_id, stock_name, stock_name_en, market, industry, is_active, created_at)
VALUES
    ('2330', '台積電',   'TSMC',                  'TWSE', '半導體',   TRUE, NOW()),
    ('2317', '鴻海',     'Hon Hai Precision',     'TWSE', '電子零組件', TRUE, NOW()),
    ('2454', '聯發科',   'MediaTek',              'TWSE', '半導體',   TRUE, NOW()),
    ('2412', '中華電',   'Chunghwa Telecom',      'TWSE', '電信',     TRUE, NOW()),
    ('1301', '台塑',     'Formosa Plastics',      'TWSE', '塑膠',     TRUE, NOW()),
    ('2308', '台達電',   'Delta Electronics',     'TWSE', '電子零組件', TRUE, NOW()),
    ('2382', '廣達',     'Quanta Computer',       'TWSE', '電腦週邊',  TRUE, NOW()),
    ('2881', '富邦金',   'Fubon Financial',       'TWSE', '金融',     TRUE, NOW()),
    ('2882', '國泰金',   'Cathay Financial',      'TWSE', '金融',     TRUE, NOW()),
    ('2886', '兆豐金',   'Mega Financial',        'TWSE', '金融',     TRUE, NOW()),
    ('3008', '大立光',   'Largan Precision',      'TWSE', '光學',     TRUE, NOW()),
    ('2002', '中鋼',     'China Steel',           'TWSE', '鋼鐵',     TRUE, NOW()),
    ('6505', '台塑化',   'Formosa Petrochemical', 'TWSE', '石化',     TRUE, NOW()),
    ('2303', '聯電',     'United Microelectronics','TWSE', '半導體',  TRUE, NOW()),
    ('6488', '環球晶',   'GlobalWafers',          'OTC',  '半導體',   TRUE, NOW()),
    ('5483', '中美晶',   'Sino-American Silicon', 'OTC',  '半導體',   TRUE, NOW()),
    ('6415', '矽力-KY',  'Silergy',               'OTC',  '半導體',   TRUE, NOW()),
    ('4966', '譜瑞-KY',  'Parade Technologies',   'OTC',  '半導體',   TRUE, NOW())
ON CONFLICT (stock_id) DO NOTHING;

/**
 * 個股相關型別定義（Wave B Round 2 對齊 Schema Lock）
 *
 * 唯一事實源：docs/03_spec/20260422_schema-lock_stock-detail-apis.md §6
 * 仲裁依據：Jamie D1-D6（2026-04-22）
 *
 * 對應後端端點：
 *   POST /api/v1/quote/get       → StockQuote
 *   POST /api/v1/quote/list      → StockQuote[]
 *   POST /api/v1/quote/history   → KLineHistory
 *   POST /api/v1/fundamental/get → StockFundamental
 *   POST /api/v1/chip/get        → StockChip
 *
 * 數值精度原則：
 *   - 金額 / 比率類（price、change 等）：後端 BigDecimal → JSON string，前端 string 保留精度
 *   - 整數類（volume、buy、sell 等）：JSON number，JavaScript safe integer 範圍內
 * 時間格式：
 *   - datetime：ISO 8601 含時區 "YYYY-MM-DDTHH:mm:ss.sss+08:00"
 *   - date：純日期 "YYYY-MM-DD"
 */

// ---- 行情 ----

export interface StockQuote {
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  /** 當日收盤 / 最新成交價（BigDecimal → string，例 "1050.00"） */
  price: string;
  /** 前一交易日收盤價（BigDecimal → string） */
  previousClose: string;
  /** 漲跌額 = price − previousClose（後端計算，例 "+19.00" / "-5.00"，正值帶 +） */
  change: string;
  /** 漲跌幅（後端計算，例 "+1.84" 表示 +1.84%，正值帶 +，不含 % 符號） */
  changePercent: string;
  /** 開盤價 */
  open: string;
  /** 當日最高價 */
  high: string;
  /** 當日最低價 */
  low: string;
  /** 成交量（股數，JavaScript safe integer） */
  volume: number;
  /** 報價日期（YYYY-MM-DD，最新交易日日期） */
  quoteDate: string;
  /** 資料最後更新時間（ISO 8601 含時區） */
  updatedAt: string;
  /**
   * 是否為陳舊資料。
   * 週末 / 假日 / 外部來源失敗時 fallback 回 DB 最新一筆並標記 true。
   * isStale=true 時前端應顯示「資料延遲」警示 Tag。
   */
  isStale: boolean;
  /** 資料來源，與 market 保持一致 */
  source: 'TWSE' | 'OTC';
}

// ---- 歷史 K 線 ----

export type KLinePeriod = 'daily' | 'weekly' | 'monthly';

export interface KLineItem {
  /** 日期（YYYY-MM-DD）；weekly 取週首交易日；monthly 取月首交易日 */
  date: string;
  /** 開盤價（BigDecimal → string） */
  open: string;
  /** 最高價（BigDecimal → string） */
  high: string;
  /** 最低價（BigDecimal → string） */
  low: string;
  /** 收盤價（BigDecimal → string） */
  close: string;
  /** 成交量（股數）；weekly/monthly 取加總 */
  volume: number;
}

export interface KLineHistory {
  stockId: string;
  period: KLinePeriod;
  /** K 線資料陣列，依 date 升冪排序 */
  items: KLineItem[];
}

/**
 * 向下相容別名：KLineChart 元件消費的是 items 陣列內容，
 * 使用 KLineItem 別名避免大規模改元件。
 * 注意：此別名與 KLineItem 完全相同，未來可直接統一為 KLineItem。
 */
export type StockHistoryItem = KLineItem;

// ---- 基本面 ----

export interface StockFundamental {
  stockId: string;
  stockName: string;
  /** 近四季 EPS 滾動加總（BigDecimal → string，例 "43.50"） */
  eps: string;
  /**
   * 本益比（BigDecimal → string，例 "24.14"）
   * 注意：欄位名稱固定為 per，禁止用 perRatio
   */
  per: string;
  /**
   * 股價淨值比（BigDecimal → string）
   * 注意：欄位名稱固定為 pbr，禁止用 pbrRatio
   */
  pbr: string;
  /** 股東權益報酬率（百分比，BigDecimal → string，例 "23.5"，不含 % 符號） */
  roe: string;
  /** 最新季報年份（例 2025） */
  reportYear: number;
  /** 最新季報季別（1 ~ 4） */
  reportQuarter: number;
  /** 資料最後更新時間（ISO 8601 含時區） */
  updatedAt: string;
  /** 資料來源（目前固定為 "MOPS"） */
  source: 'MOPS';
}

/**
 * 向下相容別名：FundamentalCard 原本使用 Fundamental 型別名。
 * Round 2 起以 StockFundamental 為規範名稱。
 */
export type Fundamental = StockFundamental;

// ---- 籌碼 ----

export type InstitutionName = '外資' | '投信' | '自營商';

export interface InstitutionItem {
  name: InstitutionName;
  /** 買進股數 */
  buy: number;
  /** 賣出股數 */
  sell: number;
  /** 買賣超（= buy − sell），正值為買超，負值為賣超 */
  netBuySell: number;
}

export interface StockChip {
  stockId: string;
  /** 資料日期（YYYY-MM-DD，最新交易日） */
  date: string;
  /** 三大法人陣列，固定 3 筆，順序：外資、投信、自營商 */
  institutions: InstitutionItem[];
  /** 三大法人合計買賣超（股數），後端計算 */
  totalNetBuySell: number;
  /** 資料來源 */
  source: 'TWSE' | 'OTC';
}

/**
 * 向下相容別名：ChipCard 原本使用 Chip / ChipInstitutionEntry 型別名。
 * Round 2 起以 StockChip / InstitutionItem 為規範名稱。
 */
export type Chip = StockChip;
export type ChipInstitutionEntry = InstitutionItem;

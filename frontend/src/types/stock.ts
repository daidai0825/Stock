/**
 * 個股相關型別定義（Wave B）
 * 對應後端 Bruno Wave 2 API：
 *   POST /api/v1/quote/get       → StockQuote
 *   POST /api/v1/quote/history   → StockHistoryItem[]
 *   POST /api/v1/fundamental/get → Fundamental
 *   POST /api/v1/chip/get        → Chip
 *
 * 金額欄位使用 string 保留精度（後端 BigDecimal → JSON string）
 * 時間欄位使用 ISO 8601 含時區（GMT+8）
 */

export interface StockQuote {
  stockId: string;
  stockName: string;
  /** 成交價，string 保留精度 */
  price: string;
  /** 漲跌額，正為漲、負為跌 */
  change: string;
  /** 漲跌幅（%），含符號，如 "+1.23" / "-0.45" */
  changePercent: string;
  volume: number;
  /** ISO 8601 + 時區，如 "2026-04-22T10:30:45.123+08:00" */
  updatedAt: string;
}

export interface StockHistoryItem {
  /** YYYY-MM-DD */
  date: string;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: number;
}

export interface Fundamental {
  stockId: string;
  /** 每股盈餘（元） */
  eps: string | null;
  /** 本益比 */
  per: string | null;
  /** 股價淨值比 */
  pbr: string | null;
  /** 股東權益報酬率（%） */
  roe: string | null;
  /** 資料更新日 YYYY-MM-DD */
  updatedAt: string;
  /** 資料來源說明 */
  source: string;
}

export interface ChipInstitutionEntry {
  /** 法人名稱：「外資」「投信」「自營商」 */
  name: string;
  /** 買賣超（股），正為買超、負為賣超 */
  netBuySell: number;
  /** 買進（股） */
  buy: number;
  /** 賣出（股） */
  sell: number;
}

export interface Chip {
  stockId: string;
  /** 資料日期 YYYY-MM-DD */
  date: string;
  institutions: ChipInstitutionEntry[];
  /** 資料來源說明 */
  source: string;
}

/** K 線週期 */
export type KLinePeriod = 'daily' | 'weekly' | 'monthly';

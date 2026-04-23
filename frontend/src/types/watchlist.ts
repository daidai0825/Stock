/**
 * 自選股模組型別（M-WATCH，Wave 3 Schema Lock）
 *
 * Wave 3 重大變更（破壞性更新）：
 *   - WatchlistItem → WatchlistListItem（欄位全面更新）
 *   - stockCode / watchId → stockId / itemId
 *   - market: 'TSE'|'OTC' → 'TWSE'|'TPEx'
 *   - 新增 WatchlistQuoteSnapshot（即時報價快照）
 *   - 新增 StockSearchItem / StockSearchResult（股票搜尋）
 *   - 移除 groupTag / sortOrder / healthScore / dataDelayMinutes
 *
 * 唯一事實源：docs/03_spec/20260422_schema-lock_stock-detail-apis.md
 */

/** 即時報價快照（Wave 3 隨 watchlist/list 回傳） */
export interface WatchlistQuoteSnapshot {
  price: string;          // 成交價，例如 "1050.00"
  change: string;         // 漲跌，例如 "+19.00"
  changePercent: string;  // 漲跌幅，例如 "+1.84"（不含 %）
  volume: number;         // 成交量（股）
  quoteDate: string;      // 報價日期 "YYYY-MM-DD"
  isStale: boolean;       // true = fallback 陳舊資料
}

/** 自選股清單項目（Wave 3） */
export interface WatchlistListItem {
  itemId: string;                          // 自選股記錄 UUID
  stockId: string;                         // 股票代號，例如 "2330"
  stockName: string;                       // 股票中文名稱
  market: 'TWSE' | 'TPEx';                // 市場別（上市/上櫃）
  createdAt: string;                       // 加入時間 ISO 8601
  /** 行情快照；若該股行情來源異常則為 null，搭配 quoteError 說明原因 */
  quote: WatchlistQuoteSnapshot | null;
  /** 行情取得失敗時的錯誤碼；quote 不為 null 時此欄為 null */
  quoteError: number | null;
}

/** POST /api/v1/watchlist/list 回應 data 段 */
export interface WatchlistListResult {
  items: WatchlistListItem[];
  total: number;
}

/** POST /api/v1/watchlist/add 請求 */
export interface AddWatchlistRequest {
  stockId: string;
}

/** POST /api/v1/watchlist/add 回應 data 段 */
export interface WatchlistAddResult {
  itemId: string;
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'TPEx';
  createdAt: string;
}

/** POST /api/v1/watchlist/remove 請求 */
export interface RemoveWatchlistRequest {
  stockId: string;
}

/** POST /api/v1/stock/search 搜尋結果項目 */
export interface StockSearchItem {
  stockId: string;
  stockName: string;
  stockNameEn: string | null;
  market: 'TWSE' | 'TPEx';
  matchType: 'ID_EXACT' | 'NAME_EXACT' | 'NAME_PREFIX' | 'NAME_CONTAINS';
}

/** POST /api/v1/stock/search 請求 */
export interface StockSearchRequest {
  keyword: string;
  limit?: number;
}

/** POST /api/v1/stock/search 回應 data 段 */
export interface StockSearchResult {
  items: StockSearchItem[];
  keyword: string;
  total: number;
}

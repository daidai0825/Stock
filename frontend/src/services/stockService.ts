import { postJson } from './http';
import type { KLineHistory, KLinePeriod, StockChip, StockFundamental, StockQuote } from '@/types/stock';

/**
 * 個股相關 API service（Wave B Round 2 對齊 Schema Lock）
 *
 * 唯一事實源：docs/03_spec/20260422_schema-lock_stock-detail-apis.md
 * 仲裁依據：Jamie D1-D6（2026-04-22）
 *
 * 端點一覽：
 *   POST /api/v1/quote/get       → StockQuote
 *   POST /api/v1/quote/history   → KLineHistory（含 stockId / period / items[]）
 *   POST /api/v1/fundamental/get → StockFundamental
 *   POST /api/v1/chip/get        → StockChip
 *
 * Round 2 變更：
 * - getHistory() 回傳改為 KLineHistory（含 stockId / period / items 包裝物件），
 *   取代舊版直接回傳 StockHistoryItem[]（Bruno 已確認後端回傳包裝物件）
 * - getHistory() request body：{ stockId, period } 對齊 D3 仲裁（廢除 { stockId, month }）
 * - getFundamental() 回傳型別改為 StockFundamental（per/pbr 不加 Ratio 字尾）
 * - getChip() 回傳型別改為 StockChip（institutions[] 陣列結構，含 buy/sell）
 *
 * 所有路徑皆走 postJson → http 攔截器 → Envelope 解封
 */

interface GetQuoteRequest {
  stockId: string;
}

interface GetHistoryRequest {
  stockId: string;
  /** 資料週期：daily / weekly / monthly（D3 仲裁：廢除舊版 month: LocalDate 語意） */
  period: KLinePeriod;
  /** 起始日期（YYYY-MM-DD），預設今日 90 天前，可選 */
  startDate?: string;
  /** 結束日期（YYYY-MM-DD），預設今日，可選 */
  endDate?: string;
}

interface GetFundamentalRequest {
  stockId: string;
}

interface GetChipRequest {
  stockId: string;
}

export const stockService = {
  getQuote(stockId: string): Promise<StockQuote> {
    return postJson<GetQuoteRequest, StockQuote>('/api/v1/quote/get', { stockId });
  },

  /**
   * 查詢個股歷史 K 線資料。
   * 回傳 KLineHistory 包裝物件（含 stockId / period / items[]）。
   * 後端依 period 自動聚合（daily/weekly/monthly），前端不自行聚合。
   */
  getHistory(
    stockId: string,
    period: KLinePeriod,
    startDate?: string,
    endDate?: string,
  ): Promise<KLineHistory> {
    const body: GetHistoryRequest = { stockId, period };
    if (startDate !== undefined) body.startDate = startDate;
    if (endDate !== undefined) body.endDate = endDate;
    return postJson<GetHistoryRequest, KLineHistory>('/api/v1/quote/history', body);
  },

  getFundamental(stockId: string): Promise<StockFundamental> {
    return postJson<GetFundamentalRequest, StockFundamental>('/api/v1/fundamental/get', { stockId });
  },

  getChip(stockId: string): Promise<StockChip> {
    return postJson<GetChipRequest, StockChip>('/api/v1/chip/get', { stockId });
  },
};

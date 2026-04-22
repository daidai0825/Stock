import { postJson } from './http';
import type { Chip, Fundamental, KLinePeriod, StockHistoryItem, StockQuote } from '@/types/stock';

/**
 * 個股相關 API service（Wave B）
 * 串接 Bruno Wave 2 後端：
 *   POST /api/v1/quote/get       → StockQuote
 *   POST /api/v1/quote/history   → StockHistoryItem[]（K 線資料源）
 *   POST /api/v1/fundamental/get → Fundamental
 *   POST /api/v1/chip/get        → Chip
 *
 * 所有路徑皆走 postJson → http 攔截器 → Envelope 解封
 */

interface GetQuoteRequest {
  stockId: string;
}

interface GetHistoryRequest {
  stockId: string;
  period: KLinePeriod;
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

  getHistory(stockId: string, period: KLinePeriod): Promise<StockHistoryItem[]> {
    return postJson<GetHistoryRequest, StockHistoryItem[]>('/api/v1/quote/history', {
      stockId,
      period,
    });
  },

  getFundamental(stockId: string): Promise<Fundamental> {
    return postJson<GetFundamentalRequest, Fundamental>('/api/v1/fundamental/get', { stockId });
  },

  getChip(stockId: string): Promise<Chip> {
    return postJson<GetChipRequest, Chip>('/api/v1/chip/get', { stockId });
  },
};

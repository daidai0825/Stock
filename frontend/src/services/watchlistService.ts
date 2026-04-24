/**
 * 自選股 Service（Wave 3 — 真實 API 呼叫）
 *
 * Wave 3 變更：
 *   - 移除 Wave A mock data 與 simulateLatency
 *   - 改用 postJson 呼叫真實 API（透過 MSW mock 在 dev 環境攔截）
 *   - 新增 stockSearchService（股票搜尋）
 *   - schema 對齊 docs/03_spec/20260422_schema-lock_stock-detail-apis.md
 *
 * API 端點（Envelope Pattern，全部 POST）：
 *   POST /api/v1/watchlist/list   → WatchlistListResult
 *   POST /api/v1/watchlist/add    → WatchlistAddResult
 *   POST /api/v1/watchlist/remove → null
 *   POST /api/v1/stock/search     → StockSearchResult
 */

import { postJson } from '@/services/http';
import type {
  AddWatchlistRequest,
  RemoveWatchlistRequest,
  WatchlistListResult,
  WatchlistAddResult,
  StockSearchRequest,
  StockSearchResult,
} from '@/types/watchlist';

export const watchlistService = {
  list(): Promise<WatchlistListResult> {
    return postJson<Record<string, never>, WatchlistListResult>('/api/v1/watchlist/list', {});
  },

  add(req: AddWatchlistRequest): Promise<WatchlistAddResult> {
    return postJson<AddWatchlistRequest, WatchlistAddResult>('/api/v1/watchlist/add', req);
  },

  remove(req: RemoveWatchlistRequest): Promise<null> {
    return postJson<RemoveWatchlistRequest, null>('/api/v1/watchlist/remove', req);
  },
};

export const stockSearchService = {
  search(keyword: string, limit = 10): Promise<StockSearchResult> {
    return postJson<StockSearchRequest, StockSearchResult>('/api/v1/stock/search', {
      keyword,
      limit,
    });
  },
};

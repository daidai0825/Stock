import type { AddWatchlistRequest, RemoveWatchlistRequest, WatchlistItem } from '@/types/watchlist';

/**
 * M-WATCH service 骨架（Wave A）
 * 對應 SRS §1.2 F-WATCH-01 ~ F-WATCH-08
 *
 * Wave A：mock data；後端 Wave 2 實作後切換真實呼叫（postJson）。
 * TODO（Wave B）：
 *   list   → POST /api/v1/watchlist/list
 *   add    → POST /api/v1/watchlist/add
 *   remove → POST /api/v1/watchlist/remove
 */

const MOCK_WATCHLIST: ReadonlyArray<WatchlistItem> = [
  {
    watchId: 'mock-watch-001',
    stockCode: '2330',
    stockName: '台積電',
    market: 'TSE',
    groupTag: '電子權值',
    sortOrder: 1,
    healthScore: 78,
    changePercent: 1.25,
    lastPrice: 1085,
    dataDelayMinutes: 20,
    createdAt: '2026-04-22T08:00:00.000+08:00',
  },
  {
    watchId: 'mock-watch-002',
    stockCode: '2317',
    stockName: '鴻海',
    market: 'TSE',
    groupTag: '電子權值',
    sortOrder: 2,
    healthScore: 65,
    changePercent: -0.45,
    lastPrice: 220,
    dataDelayMinutes: 20,
    createdAt: '2026-04-22T08:00:00.000+08:00',
  },
  {
    watchId: 'mock-watch-003',
    stockCode: '0050',
    stockName: '元大台灣50',
    market: 'TSE',
    groupTag: 'ETF',
    sortOrder: 3,
    healthScore: null,
    changePercent: 0.32,
    lastPrice: 195.5,
    dataDelayMinutes: 20,
    createdAt: '2026-04-22T08:00:00.000+08:00',
  },
];

const simulateLatency = <T>(value: T, ms = 200): Promise<T> =>
  new Promise((resolve) => {
    setTimeout(() => resolve(value), ms);
  });

export const watchlistService = {
  list(): Promise<WatchlistItem[]> {
    // Wave A mock；保持回應形狀與 Wave B 一致
    return simulateLatency<WatchlistItem[]>([...MOCK_WATCHLIST]);
  },

  add(req: AddWatchlistRequest): Promise<WatchlistItem> {
    // Wave A：僅回傳一個 mock；不真的寫入
    return simulateLatency<WatchlistItem>({
      watchId: `mock-watch-${Date.now()}`,
      stockCode: req.stockCode,
      stockName: '（待後端 Wave B 解析）',
      market: 'TSE',
      groupTag: req.groupTag ?? '預設群組',
      sortOrder: 99,
      healthScore: null,
      changePercent: null,
      lastPrice: null,
      dataDelayMinutes: 20,
      createdAt: new Date().toISOString(),
    });
  },

  remove(_req: RemoveWatchlistRequest): Promise<void> {
    return simulateLatency<void>(undefined);
  },
};

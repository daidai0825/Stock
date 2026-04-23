/**
 * MSW Watchlist Handlers（Wave 3 W1）
 *
 * 模擬 Bruno W1 後端尚未完成時的 API 回應
 * 唯一事實源：docs/03_spec/20260423_wave3_schema-lock.md §4
 *
 * 模擬情境：
 *   - watchlist/list：成功（含報價快照）
 *   - watchlist/add：成功 / 2020 已存在 / 2021 已達上限 / 4001 股票不存在 / 3001 未登入
 *   - watchlist/remove：成功 / 2023 不在清單 / 3001 未登入
 *   - stock/search：成功（依 keyword 篩選 mock 股票資料）
 */

import { http, HttpResponse } from 'msw';

const BASE = '/api/v1';

const buildEnvelope = <T>(data: T) => ({
  code: 0,
  message: 'success',
  data,
  timestamp: new Date().toISOString().replace('Z', '+08:00'),
  traceId: `mock-${Math.random().toString(36).slice(2, 10)}`,
});

const buildErrorEnvelope = (code: number, message: string) => ({
  code,
  message,
  timestamp: new Date().toISOString().replace('Z', '+08:00'),
  traceId: `mock-err-${Math.random().toString(36).slice(2, 10)}`,
});

/** 模擬股票主檔（供搜尋與 watchlist mock 使用） */
const MOCK_STOCKS = [
  { stockId: '2330', stockName: '台積電', stockNameEn: 'TSMC', market: 'TWSE' as const },
  { stockId: '2317', stockName: '鴻海', stockNameEn: 'Hon Hai', market: 'TWSE' as const },
  { stockId: '0050', stockName: '元大台灣50', stockNameEn: 'Yuanta/P-shares Taiwan Top 50 ETF', market: 'TWSE' as const },
  { stockId: '2454', stockName: '聯發科', stockNameEn: 'MediaTek', market: 'TWSE' as const },
  { stockId: '2412', stockName: '中華電', stockNameEn: 'Chunghwa Telecom', market: 'TWSE' as const },
  { stockId: '6505', stockName: '台塑化', stockNameEn: 'FPCC', market: 'TWSE' as const },
  { stockId: '8046', stockName: '南電', stockNameEn: 'Nanya New Pcb', market: 'OTC' as const },
];

/** 模擬 Watchlist（in-memory，供跨 handler 共享） */
const mockWatchlistItems = [
  {
    itemId: 'uuid-watch-001',
    stockId: '2330',
    stockName: '台積電',
    market: 'TWSE' as const,
    createdAt: '2026-04-22T09:00:00.000+08:00',
    quote: {
      price: '1050.00',
      change: '+19.00',
      changePercent: '+1.84',
      volume: 28_450_000,
      quoteDate: '2026-04-23',
      isStale: false,
    },
    quoteError: null,
  },
  {
    itemId: 'uuid-watch-002',
    stockId: '2317',
    stockName: '鴻海',
    market: 'TWSE' as const,
    createdAt: '2026-04-22T09:05:00.000+08:00',
    quote: {
      price: '218.50',
      change: '-1.50',
      changePercent: '-0.68',
      volume: 45_200_000,
      quoteDate: '2026-04-23',
      isStale: false,
    },
    quoteError: null,
  },
];

export const watchlistHandlers = [
  // POST /api/v1/watchlist/list
  http.post(`${BASE}/watchlist/list`, () => {
    return HttpResponse.json(
      buildEnvelope({
        items: mockWatchlistItems,
        total: mockWatchlistItems.length,
      }),
    );
  }),

  // POST /api/v1/watchlist/add
  http.post(`${BASE}/watchlist/add`, async ({ request }) => {
    const body = await request.json() as { stockId?: string };
    const stockId = body.stockId;

    if (stockId === undefined || stockId.trim().length === 0) {
      return HttpResponse.json(buildErrorEnvelope(1001, 'stockId 為必填參數'));
    }

    const stock = MOCK_STOCKS.find((s) => s.stockId === stockId);
    if (stock === undefined) {
      return HttpResponse.json(buildErrorEnvelope(4001, '查無此股票'));
    }

    const alreadyExists = mockWatchlistItems.some((item) => item.stockId === stockId);
    if (alreadyExists) {
      return HttpResponse.json(buildErrorEnvelope(2020, '該股票已在自選股清單'));
    }

    if (mockWatchlistItems.length >= 50) {
      return HttpResponse.json(buildErrorEnvelope(2021, '已達自選股上限 50 檔'));
    }

    return HttpResponse.json(
      buildEnvelope({
        itemId: `uuid-watch-${Date.now()}`,
        stockId: stock.stockId,
        stockName: stock.stockName,
        market: stock.market,
        createdAt: new Date().toISOString().replace('Z', '+08:00'),
      }),
    );
  }),

  // POST /api/v1/watchlist/remove
  http.post(`${BASE}/watchlist/remove`, async ({ request }) => {
    const body = await request.json() as { stockId?: string };
    const stockId = body.stockId;

    if (stockId === undefined || stockId.trim().length === 0) {
      return HttpResponse.json(buildErrorEnvelope(1001, 'stockId 為必填參數'));
    }

    const exists = mockWatchlistItems.some((item) => item.stockId === stockId);
    if (!exists) {
      return HttpResponse.json(buildErrorEnvelope(2023, '該股票不在自選股清單'));
    }

    return HttpResponse.json(buildEnvelope(null));
  }),

  // POST /api/v1/stock/search（供 AddStockModal 使用）
  http.post(`${BASE}/stock/search`, async ({ request }) => {
    const body = await request.json() as { keyword?: string; limit?: number };
    const keyword = body.keyword ?? '';
    const limit = body.limit ?? 10;

    if (keyword.trim().length === 0) {
      return HttpResponse.json(buildErrorEnvelope(1001, 'keyword 為必填參數'));
    }

    if (keyword.length > 50) {
      return HttpResponse.json(buildErrorEnvelope(1004, 'keyword 超過 50 字元上限'));
    }

    const kw = keyword.toLowerCase();
    const matched = MOCK_STOCKS.filter(
      (s) =>
        s.stockId.startsWith(keyword) ||
        s.stockName.includes(keyword) ||
        (s.stockNameEn?.toLowerCase().startsWith(kw) ?? false),
    ).slice(0, limit);

    const items = matched.map((s) => ({
      ...s,
      matchType: s.stockId === keyword
        ? 'ID_EXACT'
        : s.stockId.startsWith(keyword)
          ? 'ID_PREFIX'
          : s.stockName === keyword
            ? 'NAME_EXACT'
            : s.stockName.startsWith(keyword)
              ? 'NAME_PREFIX'
              : 'NAME_PARTIAL',
    }));

    return HttpResponse.json(
      buildEnvelope({ items, keyword, total: items.length }),
    );
  }),
];

/**
 * 錯誤情境 handlers（供測試專用，不在 dev 預設啟用）
 */
export const watchlistErrorHandlers = {
  /** 3001 未登入 */
  unauthorized: [
    http.post(`${BASE}/watchlist/list`, () =>
      HttpResponse.json(buildErrorEnvelope(3001, '未登入，請先登入')),
    ),
    http.post(`${BASE}/watchlist/add`, () =>
      HttpResponse.json(buildErrorEnvelope(3001, '未登入，請先登入')),
    ),
    http.post(`${BASE}/watchlist/remove`, () =>
      HttpResponse.json(buildErrorEnvelope(3001, '未登入，請先登入')),
    ),
  ],
  /** 2020 重複加入 */
  alreadyExists: [
    http.post(`${BASE}/watchlist/add`, () =>
      HttpResponse.json(buildErrorEnvelope(2020, '該股票已在自選股清單')),
    ),
  ],
  /** 2021 已達上限 */
  limitReached: [
    http.post(`${BASE}/watchlist/add`, () =>
      HttpResponse.json(buildErrorEnvelope(2021, '自選股已達上限 50 檔')),
    ),
  ],
  /** 4001 股票不存在 */
  stockNotFound: [
    http.post(`${BASE}/watchlist/add`, () =>
      HttpResponse.json(buildErrorEnvelope(4001, '查無此股票')),
    ),
  ],
  /** 2023 不在清單 */
  watchlistNotFound: [
    http.post(`${BASE}/watchlist/remove`, () =>
      HttpResponse.json(buildErrorEnvelope(2023, '該股票不在自選股清單')),
    ),
  ],
};

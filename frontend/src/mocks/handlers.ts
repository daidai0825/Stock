/**
 * MSW（Mock Service Worker）Request Handlers（Wave B Round 2 對齊 Schema Lock）
 *
 * 唯一事實源：docs/03_spec/20260422_schema-lock_stock-detail-apis.md
 *
 * 用途：Bruno Wave 2 後端 API 尚未上線時，在 dev 環境提供假資料，
 * 讓前端可獨立開發與測試。
 *
 * 啟用條件：VITE_MSW_ENABLED=true（.env.local）
 * 啟動入口：main.tsx 在 dev 模式下動態 import mocks/browser.ts
 *
 * Round 2 對齊變更：
 * - quote mock 補上 previousClose / market / open / high / low / quoteDate / isStale / source
 * - fundamental mock 改用 per/pbr（去掉 Ratio）、補 stockName/reportYear/reportQuarter、
 *   source 固定 "MOPS"、updatedAt 改為 ISO 8601 datetime
 * - chip mock 改為 institutions[] 陣列結構，補 totalNetBuySell / source
 * - history mock 改為 KLineHistory 包裝物件（含 stockId / period / items[]）
 * - 備妥 daily / weekly / monthly 三種 period 的 history mock
 * - errorCode mock 使用 5010/5011/5012（廢除 5101）
 */

import { http, HttpResponse } from 'msw';
import { watchlistHandlers } from './handlers/watchlistHandlers';

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

// 台積電 2330 基礎報價（完整 schema-lock §1.3）
const mockQuote2330 = {
  stockId: '2330',
  stockName: '台積電',
  market: 'TWSE' as const,
  price: '1050.00',
  previousClose: '1031.00',
  change: '+19.00',
  changePercent: '+1.84',
  open: '1035.00',
  high: '1055.00',
  low: '1030.00',
  volume: 28_450_000,
  quoteDate: '2026-04-22',
  updatedAt: '2026-04-22T13:30:00.000+08:00',
  isStale: false,
  source: 'TWSE' as const,
};

/** 產生模擬日線 K 線資料（N 天） */
const generateDailyItems = (days: number) => {
  const items = [];
  const today = new Date('2026-04-22');
  let price = 1031;

  for (let i = days; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    if (d.getDay() === 0 || d.getDay() === 6) continue;

    const delta = (Math.random() - 0.48) * 20;
    const open = price;
    const close = Math.max(100, price + delta);
    const high = Math.max(open, close) + Math.random() * 10;
    const low = Math.min(open, close) - Math.random() * 10;
    price = close;

    items.push({
      date: d.toISOString().slice(0, 10),
      open: open.toFixed(2),
      high: high.toFixed(2),
      low: Math.max(1, low).toFixed(2),
      close: close.toFixed(2),
      volume: Math.floor(Math.random() * 50_000_000 + 10_000_000),
    });
  }
  return items;
};

/** 產生模擬週線 K 線資料（N 週） */
const generateWeeklyItems = (weeks: number) => {
  const items = [];
  let price = 1031;
  const baseDate = new Date('2026-04-22');

  for (let i = weeks; i >= 0; i--) {
    const d = new Date(baseDate);
    d.setDate(d.getDate() - i * 7);
    // 對齊至週一
    const day = d.getDay();
    d.setDate(d.getDate() - (day === 0 ? 6 : day - 1));

    const delta = (Math.random() - 0.48) * 60;
    const open = price;
    const close = Math.max(100, price + delta);
    const high = Math.max(open, close) + Math.random() * 30;
    const low = Math.min(open, close) - Math.random() * 30;
    price = close;

    items.push({
      date: d.toISOString().slice(0, 10),
      open: open.toFixed(2),
      high: high.toFixed(2),
      low: Math.max(1, low).toFixed(2),
      close: close.toFixed(2),
      volume: Math.floor(Math.random() * 200_000_000 + 50_000_000),
    });
  }
  return items;
};

/** 產生模擬月線 K 線資料（N 個月） */
const generateMonthlyItems = (months: number) => {
  const items = [];
  let price = 1031;
  const baseDate = new Date('2026-04-01');

  for (let i = months; i >= 0; i--) {
    const d = new Date(baseDate.getFullYear(), baseDate.getMonth() - i, 1);
    const delta = (Math.random() - 0.48) * 150;
    const open = price;
    const close = Math.max(100, price + delta);
    const high = Math.max(open, close) + Math.random() * 60;
    const low = Math.min(open, close) - Math.random() * 60;
    price = close;

    items.push({
      date: d.toISOString().slice(0, 10),
      open: open.toFixed(2),
      high: high.toFixed(2),
      low: Math.max(1, low).toFixed(2),
      close: close.toFixed(2),
      volume: Math.floor(Math.random() * 800_000_000 + 200_000_000),
    });
  }
  return items;
};

export const handlers = [
  // Watchlist handlers（Wave 3 W1）
  ...watchlistHandlers,

  // POST /api/v1/quote/get
  http.post(`${BASE}/quote/get`, () => {
    return HttpResponse.json(buildEnvelope(mockQuote2330));
  }),

  // POST /api/v1/quote/history
  // 解析 period 欄位，回傳對應聚合資料（KLineHistory 包裝物件）
  // M-FE-WaveB-R2-03 修正：period 須通過白名單驗證，未在白名單者回 1003 PARAM_OUT_OF_RANGE
  // 與真實後端行為對齊（schema-lock §3.3 KLinePeriod = 'daily' | 'weekly' | 'monthly'）
  http.post(`${BASE}/quote/history`, async ({ request }) => {
    const body = await request.json() as { stockId?: string; period?: string };
    const stockId = body.stockId ?? '2330';
    const period = body.period ?? 'daily';

    const allowedPeriods = ['daily', 'weekly', 'monthly'] as const;
    type AllowedPeriod = typeof allowedPeriods[number];
    if (!allowedPeriods.includes(period as AllowedPeriod)) {
      return HttpResponse.json(
        buildErrorEnvelope(1003, `period must be one of: ${allowedPeriods.join(', ')}`),
      );
    }

    let items: ReturnType<typeof generateDailyItems>;
    if (period === 'weekly') {
      items = generateWeeklyItems(52);
    } else if (period === 'monthly') {
      items = generateMonthlyItems(24);
    } else {
      // daily（預設）
      items = generateDailyItems(90);
    }

    return HttpResponse.json(
      buildEnvelope({
        stockId,
        period,
        items,
      }),
    );
  }),

  // POST /api/v1/fundamental/get（schema-lock §4.3）
  http.post(`${BASE}/fundamental/get`, () => {
    return HttpResponse.json(
      buildEnvelope({
        stockId: '2330',
        stockName: '台積電',
        eps: '43.50',
        per: '24.14',          // 注意：per 非 perRatio
        pbr: '8.72',           // 注意：pbr 非 pbrRatio
        roe: '23.5',
        reportYear: 2025,
        reportQuarter: 4,
        updatedAt: '2026-04-22T08:00:00.000+08:00',
        source: 'MOPS' as const,
      }),
    );
  }),

  // POST /api/v1/chip/get（schema-lock §5.3）
  http.post(`${BASE}/chip/get`, () => {
    return HttpResponse.json(
      buildEnvelope({
        stockId: '2330',
        date: '2026-04-22',
        institutions: [
          { name: '外資' as const, buy: 18_500_000, sell: 12_000_000, netBuySell: 6_500_000 },
          { name: '投信' as const, buy: 3_200_000,  sell: 1_500_000,  netBuySell: 1_700_000 },
          { name: '自營商' as const, buy: 2_100_000, sell: 3_800_000, netBuySell: -1_700_000 },
        ],
        totalNetBuySell: 6_500_000,
        source: 'TWSE' as const,
      }),
    );
  }),
];

/**
 * 錯誤情境 handlers（供測試使用，不在 dev 預設啟用）
 * 使用方式：server.use(...errorHandlers.twseUnavailable)
 */
export const errorHandlers = {
  /** 5010 TWSE_DATA_SOURCE_ERROR */
  twseUnavailable: [
    http.post(`${BASE}/quote/get`, () =>
      HttpResponse.json(buildErrorEnvelope(5010, 'TWSE data source is temporarily unavailable')),
    ),
  ],
  /** 5011 MOPS_DATA_SOURCE_ERROR */
  mopsUnavailable: [
    http.post(`${BASE}/fundamental/get`, () =>
      HttpResponse.json(buildErrorEnvelope(5011, 'MOPS data source is temporarily unavailable')),
    ),
  ],
  /** 5012 CHIP_DATA_SOURCE_ERROR */
  chipUnavailable: [
    http.post(`${BASE}/chip/get`, () =>
      HttpResponse.json(buildErrorEnvelope(5012, 'Chip data source is temporarily unavailable')),
    ),
  ],
  /** 5013 OTC_DATA_SOURCE_ERROR（OTC 行情來源故障，覆蓋 quote/history） */
  otcUnavailable: [
    http.post(`${BASE}/quote/get`, () =>
      HttpResponse.json(buildErrorEnvelope(5013, 'OTC data source is temporarily unavailable')),
    ),
    http.post(`${BASE}/quote/history`, () =>
      HttpResponse.json(buildErrorEnvelope(5013, 'OTC data source is temporarily unavailable')),
    ),
  ],
  /** 5014 MOPS_FORMAT_CHANGED（MOPS 格式變動，影響 fundamental 解析） */
  mopsFormatChanged: [
    http.post(`${BASE}/fundamental/get`, () =>
      HttpResponse.json(buildErrorEnvelope(5014, 'MOPS response format has changed; parser needs update')),
    ),
  ],
  /** isStale=true 情境（模擬 fallback 陳舊資料） */
  staleQuote: [
    http.post(`${BASE}/quote/get`, () =>
      HttpResponse.json(
        buildEnvelope({
          ...mockQuote2330,
          isStale: true,
          quoteDate: '2026-04-19',   // 上一個交易日
          updatedAt: '2026-04-19T13:30:00.000+08:00',
        }),
      ),
    ),
  ],
};

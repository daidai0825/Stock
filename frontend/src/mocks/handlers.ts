/**
 * MSW（Mock Service Worker）Request Handlers（Wave B）
 *
 * 用途：Bruno Wave 2 後端 API 尚未上線時，在 dev 環境提供假資料，
 * 讓前端可獨立開發與測試。
 *
 * 啟用條件：VITE_MSW_ENABLED=true（.env.local）
 * 啟動入口：main.tsx 在 dev 模式下動態 import mocks/browser.ts
 *
 * 假資料設計：以台積電（2330）為代表性資料。
 */

import { http, HttpResponse } from 'msw';

const BASE = '/api/v1';

const buildEnvelope = <T>(data: T) => ({
  code: 0,
  message: 'success',
  data,
  timestamp: new Date().toISOString(),
  traceId: `mock-${Math.random().toString(36).slice(2, 10)}`,
});

/** 產生模擬 K 線資料（90 天日線） */
const generateHistoryItems = (days: number) => {
  const items = [];
  const today = new Date();
  let price = 900;

  for (let i = days; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    // 跳過週末
    if (d.getDay() === 0 || d.getDay() === 6) continue;

    const change = (Math.random() - 0.48) * 20;
    const open = price;
    const close = Math.max(100, price + change);
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

export const handlers = [
  // POST /api/v1/quote/get
  http.post(`${BASE}/quote/get`, () => {
    return HttpResponse.json(
      buildEnvelope({
        stockId: '2330',
        stockName: '台積電',
        price: '912.00',
        change: '+12.00',
        changePercent: '+1.33',
        volume: 28_745_321,
        updatedAt: new Date().toISOString().replace('Z', '+08:00'),
      }),
    );
  }),

  // POST /api/v1/quote/history
  http.post(`${BASE}/quote/history`, () => {
    return HttpResponse.json(buildEnvelope(generateHistoryItems(90)));
  }),

  // POST /api/v1/fundamental/get
  http.post(`${BASE}/fundamental/get`, () => {
    return HttpResponse.json(
      buildEnvelope({
        stockId: '2330',
        eps: '32.34',
        per: '28.20',
        pbr: '7.85',
        roe: '27.83',
        updatedAt: new Date().toISOString().slice(0, 10),
        source: '公開資訊觀測站（MOPS）・每季更新',
      }),
    );
  }),

  // POST /api/v1/chip/get
  http.post(`${BASE}/chip/get`, () => {
    return HttpResponse.json(
      buildEnvelope({
        stockId: '2330',
        date: new Date().toISOString().slice(0, 10),
        institutions: [
          { name: '外資', netBuySell: 12_543_000, buy: 35_000_000, sell: 22_457_000 },
          { name: '投信', netBuySell: -2_100_000, buy: 5_000_000, sell: 7_100_000 },
          { name: '自營商', netBuySell: 890_000, buy: 3_200_000, sell: 2_310_000 },
        ],
        source: '台灣證券交易所（TWSE）・每日盤後更新',
      }),
    );
  }),
];

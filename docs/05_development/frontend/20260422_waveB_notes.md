# Wave B 前端開發筆記

**日期**：2026-04-22
**作者**：Felix（dev-frontend-felix）
**波段**：Wave B — 個股詳情頁 + K 線整合

---

## 實作範圍

| 項目 | 路徑 | 說明 |
|------|------|------|
| 型別定義 | `src/types/stock.ts` | StockQuote / StockHistoryItem / Fundamental / Chip / KLinePeriod |
| Service 層 | `src/services/stockService.ts` | 四支 API 對接（getQuote / getHistory / getFundamental / getChip） |
| Token 抽象 | `src/services/tokenStorage.ts` | FE TD-3 phase 1：TokenStorage 介面，localStorage 過渡實作 |
| MSW Handlers | `src/mocks/handlers.ts` | dev fallback 假資料（台積電 2330）|
| MSW Browser | `src/mocks/browser.ts` | 瀏覽器環境 Service Worker 設定 |
| MSW Server | `src/mocks/server.ts` | Vitest 測試環境 Node Server |
| Logger | `src/utils/logger.ts` | console.* 唯一合法封裝點，prod 靜默 debug/info |
| 路由更新 | `src/routes/AppRouter.tsx` | 加入 `/stocks/:stockId`（lazy-loaded + Suspense） |
| 路由常數 | `src/constants/routes.ts` | 新增 STOCK_DETAIL + buildStockDetailPath() |
| i18n | `zh-TW.json` / `en.json` | 新增 stock.* 命名空間 |
| StockDetail 主頁 | `src/pages/StockDetail/index.tsx` | 組合所有區塊 |
| PriceHeader | `src/pages/StockDetail/components/PriceHeader.tsx` | 即時行情標題 |
| KLineChart | `src/pages/StockDetail/components/KLineChart.tsx` | lightweight-charts 4.2 Candlestick + Volume |
| FundamentalCard | `src/pages/StockDetail/components/FundamentalCard.tsx` | 基本面卡片 |
| ChipCard | `src/pages/StockDetail/components/ChipCard.tsx` | 三大法人籌碼表格 |
| PeriodSelector | `src/pages/StockDetail/components/PeriodSelector.tsx` | 日/週/月切換 |
| Hooks（4 個） | `src/pages/StockDetail/hooks/` | useStockQuote / useStockHistory / useFundamental / useChip |
| 測試 | `src/pages/StockDetail/StockDetail.test.tsx` | 19 個測試案例全綠 |

---

## 關鍵設計決策

### 1. KLineChart canvas 與 jsdom 的隔離策略

lightweight-charts 使用 Canvas API，jsdom 無法模擬。採取「元件整合測試中 mock KLineChart」策略：

```typescript
vi.mock('./components/KLineChart', () => ({
  KLineChart: () => <div data-testid="kline-chart-mock">K 線圖（已 mock）</div>,
}));
```

KLineChart 本身的功能驗證（resize、data update、cleanup）需在 E2E（Playwright）中覆蓋，
交由 Wave B QA（Quincy / Quinn）排入 E2E 計畫。

### 2. KLineChart 記憶體釋放（chart.remove()）

```typescript
useEffect(() => {
  initChart();
  return () => {
    if (chartRef.current !== null) {
      chartRef.current.remove(); // 必要：避免 memory leak
      chartRef.current = null;
    }
  };
}, []);
```

chart 初始化與清理分離，資料更新走獨立 useEffect，避免重複 initChart。

### 3. ResizeObserver 響應式

不使用 window.resize event（精確度差），改用 ResizeObserver 監聽容器 contentRect.width，
在 unmount 時呼叫 `observer.disconnect()`。

### 4. PriceHeader props 簡化

原始設計含 `stockId` prop，但 PriceHeader 所需的股號/股名已包含在 `StockQuote.stockId / stockName`，
移除冗餘 prop（TypeScript noUnusedParameters 強制揪出）。

### 5. 台灣股市漲跌顏色慣例

台灣股市慣例：漲 = 紅色（#cf1322）、跌 = 綠色（#3f8600）。
與國際市場（漲綠跌紅）相反，需特別注意。
ChipCard 買賣超正值同樣用紅色，負值用綠色。

### 6. tokenStorage 設計

TokenStorage 介面故意薄，不重複儲存，直接讀寫 Zustand persist key（`stock-auth-store`）的 JSON 結構。
authStore 仍是 source of truth，tokenStorage 只是給非 React 樹（axios 攔截器）同步讀取的橋接。

Wave C/D 切換為 httpOnly cookie 時，只需替換 `localStorageTokenStorage` 實作，
介面呼叫端（目前無直接使用者，為預置設計）零改動。

### 7. MSW 安裝與啟用

MSW 2.x 在 Wave A 未安裝，Wave B 補裝（`msw@2.13.4`）。
測試環境使用 `setupServer`（Node），瀏覽器 dev 環境使用 `setupWorker`（需 `npx msw init public/`）。

Service Worker 公開檔案（`public/mockServiceWorker.js`）需手動執行 `npx msw init public/ --save` 產生，
不納入 git（已加至 .gitignore 建議清單）。

---

## 合規檢查

| 項目 | 狀態 | 備註 |
|------|------|------|
| K 線圖下方 disclaimer | 通過 | `t('stock.klineDisclaimer')` i18n 管理 |
| 基本面卡片 source tooltip | 通過 | InfoCircleOutlined + Tooltip，含資料來源與更新頻率 |
| 籌碼卡片 source tooltip | 通過 | 同上 |
| Footer disclaimer | 通過 | Wave A AppLayout 已有，沿用不動 |
| 禁用詞檢查 | 手動確認 | 無「數據」「組件」「接口」「對象」等中國用語 |

---

## 已知 Warnings（非 Blocker）

| Warning | 原因 | 處置 |
|---------|------|------|
| `getComputedStyle not implemented` | jsdom 不支援 antd Table scrollbar detection | 已知限制，測試仍通過；E2E 覆蓋 |
| React Router future flag v7 | Wave A 已知 warning | 留至 Wave C 統一升級 |

---

## 檢查結果

```
npm run lint      → 0 errors, 0 warnings
npm run type-check → 0 errors
npm run test --run → 34/34 passed
```

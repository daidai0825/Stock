# Wave B Round 1 修補筆記

| 項目 | 內容 |
|------|------|
| 日期 | 2026-04-22 |
| 作者 | Felix（資深前端工程師） |
| 範圍 | Fiona Wave B Review NO-GO 修補（Round 1：不含 DTO/errorCode 對齊） |
| 對應 Review | `docs/06_review/frontend/20260422_review_waveB.md` |
| 前置 | Jamie 仲裁：不需等 Peter SRS lock 的部分先修，Round 2 全力對齊 DTO/errorCode |

---

## 1. Blocker 修補

### B-FE-WaveB-01：useQueryErrorNotification 違反 Hook Rules

**問題**：副作用直接寫在 render body，每次 re-render 都 fire `notification.error`。

**修正**：在 `useEffect([error, context, t])` 裡包裝，確保只在 error 物件參考改變時觸發一次。

**關鍵設計**：
- `if (error === null) return;` 是 effect 內的 early return，不違反 Hook Rules
- 依賴陣列包含 `error`（物件參考）、`context`（字串）、`t`（穩定 i18n 函式）
- React Query retry 耗盡後 error 物件不再改變，所以不會重複觸發

**isStockNotFound 輔助函式**：新增純函式判斷是否為 4001 錯誤，避免在 render 裡寫複雜的 `instanceof BusinessError && code === 4001`。

**Hook 呼叫順序修正**：原本在 `Navigate` return 之後還有 hook 呼叫，違反 React Hook Rules（conditional after early return）。修正為：4 個 hook 全部無條件呼叫在最上層，判斷邏輯在 hook 之後。

**修改檔案**：`frontend/src/pages/StockDetail/index.tsx`

---

## 2. Major 修補

### M-FE-WaveB-01：tokenStorage 真接入 http.ts

`http.ts:73` 從直接讀 `useAuthStore.getState().token` 改為 `tokenStorage.get()`。

`tokenStorage.ts` 內部讀 Zustand persist JSON，實作不變，只是現在有真實 caller。

未來換 httpOnly cookie 時只需修改 `tokenStorage.ts` 一個地方，`http.ts` 零改動。

**修改檔案**：`frontend/src/services/http.ts`

### M-FE-WaveB-02：MSW dev fallback 真在 dev 啟動

**main.tsx** 新增 `enableMocks()` async function，條件為 `DEV && VITE_MSW_ENABLED === 'true'`。

動態 import 確保 `msw/browser` 不進 prod bundle（Vite 靜態分析 `import.meta.env.DEV` 的 if 分支）。

`package.json` 加 `"prepare": "msw init public/ --save"`，首次 `npm install` 自動產生 `mockServiceWorker.js`。

`.env.local` 補 `VITE_MSW_ENABLED=false`（開發者明確 opt-in，預設不啟用避免干擾正常開發）。

`vite-env.d.ts` 補 `VITE_MSW_ENABLED: string` 型別宣告。

**修改檔案**：`frontend/src/main.tsx`、`frontend/package.json`、`frontend/.env.local`、`frontend/src/vite-env.d.ts`

### M-FE-WaveB-03：StockDetail.test.tsx 改成真整合測試

原測試問題：
1. 整合測試 describe 裡只 render 了 `<PriceHeader>`，名不符實
2. 4 支 hook 無任何 `renderHook` 測試
3. MSW Node 攔截 + axios XHR 的相容問題（jsdom 環境）

修正策略：
- **整合測試**：用 `vi.mock('@/services/stockService')` 替換 HTTP 層，透過 `mockResolvedValue` 注入假資料。`renderStockDetailPage` helper 用 `MemoryRouter + Routes + Route path="/stocks/:stockId"` 讓 `useParams` 能正確拿到 stockId。
- **Hook 單元測試**：同樣 mock stockService，`renderHook` 測試 loading / success / error 三種狀態。
- **describe 名稱**：原 `StockDetailPage 錯誤處理` → 改為 `PriceHeader 無資料狀態降級顯示`。

總計：35 個測試，覆蓋整合 happy path、4001 錯誤路徑、4 支 hook（各 3 個）、元件單元測試。

**修改檔案**：`frontend/src/pages/StockDetail/StockDetail.test.tsx`

### M-FE-WaveB-04：StockDetail 缺空 stockId / 找不到股票 UX

- `stockId.length === 0` → `<Navigate to={RoutePath.HOME} replace />`
- `isStockNotFound(error)` 返回 true（code === 4001）→ `<div data-testid="stock-not-found-result"><Result status="404" .../></div>`
- i18n key `stock.notFound`（已存在）正確使用
- antd `Result` 不透傳 `data-testid`，改用外層 div 包裹（測試可靠性）

**修改檔案**：`frontend/src/pages/StockDetail/index.tsx`

### M-FE-WaveB-05：buildStockDetailPath 接入 Watchlist row click

Watchlist `stockCode` 欄位加 `<Link to={buildStockDetailPath(stockCode)}>`，結束了 `buildStockDetailPath` 只有宣告無 caller 的狀態。

**修改檔案**：`frontend/src/pages/Watchlist/index.tsx`

---

## 3. Minor 修補

### m-FE-WaveB-01：KLineChart chartHeight 改模組常數

`useState(400)` → 頂層 `const CHART_HEIGHT = 400`，移除無意義的 state（height 不需要觸發 re-render）。連帶移除 `useState` import（改為 useCallback + useEffect + useRef）。

**修改檔案**：`frontend/src/pages/StockDetail/components/KLineChart.tsx`

### m-FE-WaveB-02：box-drawing 超寬字元行移除

`StockDetail.test.tsx` 全檔重寫，新版本使用 `// ─── XXX ───` 格式（單一 Unicode dash），行寬符合 120 字元限制。

### m-FE-WaveB-06：PriceHeader parseFloat 改 startsWith

```ts
// 前
const num = parseFloat(change);
if (isNaN(num) || num === 0) return 'flat';
return num > 0 ? 'up' : 'down';

// 後
const trimmed = change.trim();
if (trimmed.startsWith('+') && trimmed !== '+0' && trimmed !== '+0.00') return 'up';
if (trimmed.startsWith('-')) return 'down';
return 'flat';
```

只決定方向不參與計算，純字串比較無精度問題且語意更清晰。

**修改檔案**：`frontend/src/pages/StockDetail/components/PriceHeader.tsx`

---

## 4. 不在本輪修補範圍（等 Peter SRS lock → Round 2）

- B-FE-WaveB-02：types/stock.ts 結構調整
- errorCodes.ts 5101 → 5010 對齊
- stockService.ts request body 對齊
- MSW handlers schema 對齊
- KLineChart dark mode（Wave C）

---

## 5. 最終機械檢查結果

| 檢查項目 | 結果 |
|----------|------|
| type-check | 0 errors |
| lint | 0 warnings |
| vitest | 50/50 passed（新增 16 個測試） |
| 禁用詞檢查 | 0 違規 |
| any / ts-ignore / non-null assertion | 0 |

---

## 6. 技術決策補充

**為何整合測試改 mock service 而非用 MSW Node？**

jsdom 環境下 axios 使用 XMLHttpRequest，MSW v2 Node adapter 使用 `@mswjs/interceptors` 的 http module，兩者有相容性問題（測試執行時出現 `AggregateError` + XHR not implemented）。mock service 是在 React Query 層以上替換資料來源，更接近「單元隔離」的哲學，且不依賴 HTTP 層。整合測試目的是驗證「元件接到正確資料後渲染正確」，mock service 完全足夠。

**antd Result 不支援 data-testid**：antd v5 Result 元件的 props 是 Result 型別，不透傳 HTML attributes 到根 div。改用外層 `<div data-testid>` 包裹，既不影響視覺也讓測試可靠。

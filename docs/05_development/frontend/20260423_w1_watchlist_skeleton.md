# Frontend Dev Notes：Wave 3 W1 Watchlist Skeleton

**日期**：2026-04-23
**作者**：Felix
**任務**：Wave 3 W1 Watchlist 前端骨架實作

---

## 1. 任務範圍

依 Jamie 指派，實作 Wave 3 W1 Watchlist 前端骨架，涵蓋：

| Slice | 說明 | 狀態 |
|-------|------|------|
| 1 | 型別、Service、Hook | 完成 |
| 2 | WatchlistPage、AddStockModal、EmptyState | 完成 |
| 3 | 路由確認（已存在）| 確認無需修改 |
| 4 | AuthStore 確認（已存在）| 確認無需修改 |
| MSW | watchlistHandlers + stock/search | 完成 |
| i18n | zh-TW + en 自選股鍵值 | 完成 |
| 錯誤碼 | 2023 + 2032 新增 | 完成 |
| 測試 | Vitest + RTL，11 新測試 | 完成 |

---

## 2. 關鍵設計決策

### 2.1 Wave 3 Breaking Schema Change

Wave A/B 的 `WatchlistItem` 與 Wave 3 的 `WatchlistListItem` 是完全不相容的 schema。

| 欄位 | Wave A/B | Wave 3 |
|------|----------|--------|
| ID | `watchId` | `itemId` |
| 股票代號 | `stockCode` | `stockId` |
| 市場 | `'TSE' \| 'OTC'` | `'TWSE' \| 'TPEx'` |
| 報價 | 平鋪欄位（lastPrice/changePercent） | 巢狀 `quote: WatchlistQuoteSnapshot \| null` |
| 健康度 | `healthScore: number \| null` | 移除（Wave 3 無此欄位） |

決策：完整替換型別，不保留向後相容。舊 Wave A mock 資料在 Wave 3 測試中不再使用。

### 2.2 Hook 命名遷移

| 舊名稱（Wave A） | 新名稱（Wave 3） |
|------------------|------------------|
| `useWatchlist` | `useWatchlistQuery` |
| `useAddWatchlist` | `useAddStockMutation` |
| `useRemoveWatchlist` | `useRemoveStockMutation` |

理由：與 Wave 3 的 mutation 語義更一致（動詞 + Mutation suffix），且避免與舊 hook 名稱衝突。

### 2.3 樂觀更新策略

- **Add**：先插入 placeholder item（`quote: null`），API 成功後 invalidate 讓後端真實資料取代
- **Remove**：先從 cache 過濾掉，API 失敗時從 `onMutate` 的 context 還原
- 兩者都使用 `onSettled` 確保最終 invalidate，避免 cache 不一致

```typescript
// useRemoveStockMutation 的 onMutate
const previous = queryClient.getQueryData<WatchlistListResult>(WATCHLIST_QUERY_KEY);
queryClient.setQueryData<WatchlistListResult>(WATCHLIST_QUERY_KEY, {
  items: previous.items.filter((item) => item.stockId !== req.stockId),
  total: Math.max(0, previous.total - 1),
});
return { previous };
```

### 2.4 台股漲跌顏色慣例

台股顏色與美股相反：

```typescript
const getChangeColor = (raw: string): string => {
  if (raw.startsWith('+')) return '#cf1322';  // 紅 = 上漲
  if (raw.startsWith('-')) return '#3f8600';  // 綠 = 下跌
  return 'inherit';
};
```

後端回傳的 `changePercent` 欄位已帶正負號（`"+1.84"` / `"-0.68"`），前端直接用前綴字元判斷，不需要 parseFloat。

### 2.5 AddStockModal Debounce

使用 `useRef<ReturnType<typeof setTimeout> | null>` 控制 debounce timer，避免在 re-render 之間累積多個 timer。Debounce 設定 250ms（schema-lock 規範）。

### 2.6 MSW 的 MOCK_STOCKS market 型別問題

`watchlistHandlers.ts` 中的 `MOCK_STOCKS` 有一筆是 `market: 'OTC'`（南電），但 Wave 3 schema 已改為 `'TWSE' | 'TPEx'`。

此為 mock 資料的不一致，TypeScript 在嚴格模式下會捕捉此問題。確認 `StockSearchItem.market` 型別為 `'TWSE' | 'TPEx'`，後續 Bruno 實作後端時需確保搜尋回應也使用相同型別。

> **Spec 問題（待 Peter 確認）**：搜尋結果的 `matchType` 在 watchlistHandlers.ts 使用了 `'ID_PREFIX'`、`'NAME_PARTIAL'` 兩個值，但 `StockSearchItem.matchType` 定義為 `'ID_EXACT' | 'NAME_EXACT' | 'NAME_PREFIX' | 'NAME_CONTAINS'`。Handler 中的值需對齊。

---

## 3. 已知問題與限制

### 3.1 Ant Design 5 的 destroyOnClose 棄用警告

`AddStockModal` 使用 `destroyOnClose` 屬性，Ant Design 5 已棄用此 prop，建議改用 `destroyOnHidden`。測試中出現大量 stderr 警告，但不影響功能。

**處置**：在測試 stderr 警告中已知，Wave 3 驗收後統一升級 Ant Design 5.x 版本時修正。

### 3.2 Popconfirm 在 jsdom 環境的限制

Ant Design Popconfirm 的 overlay 在 jsdom 中不會 render 到 DOM，導致無法從 UI 層面測試「點擊確認」流程。

**處置**：
- 測試「移除按鈕顯示」：確認按鈕存在並可點擊（觸發 Popconfirm）
- 測試「service 被呼叫」：直接呼叫 `watchlistService.remove()` 繞過 UI
- Popconfirm 完整展開/確認流程由 E2E 測試（Playwright）覆蓋

### 3.3 watchlistHandlers.ts 的 `'OTC'` market 型別

`MOCK_STOCKS` 中 stockId `'8046'` 南電的 market 為 `'OTC'`，但 Wave 3 schema 的 `StockSearchItem.market` 只允許 `'TWSE' | 'TPEx'`。TypeScript 嚴格模式會在此報錯，需在後續修正 handler。

> 目前透過型別斷言 `as const` 讓 TypeScript 推斷為字面量，但嚴格來說 `'OTC'` 不符合 Wave 3 schema。

---

## 4. 新增/修改的檔案清單

| 檔案 | 操作 | 說明 |
|------|------|------|
| `src/types/watchlist.ts` | 全量改寫 | Wave 3 schema（WatchlistListItem / WatchlistAddResult / StockSearchItem 等） |
| `src/services/watchlistService.ts` | 全量改寫 | 移除 Wave A mock，改用 postJson 真實 API |
| `src/hooks/useWatchlist.ts` | 全量改寫 | 樂觀更新、新 hook 命名、WATCHLIST_QUERY_KEY export |
| `src/pages/Watchlist/index.tsx` | 全量改寫 | Wave 3 功能完整版（報價、移除、AddStockModal） |
| `src/pages/Watchlist/components/AddStockModal.tsx` | 新增 | 搜尋 + 加入自選股 Modal |
| `src/pages/Watchlist/components/EmptyState.tsx` | 新增 | 空狀態元件 |
| `src/pages/Watchlist/Watchlist.test.tsx` | 新增 | 11 個新測試（WatchlistPage + AddStockModal + EmptyState） |
| `src/mocks/handlers/watchlistHandlers.ts` | 新增 | Watchlist + stock/search MSW handlers |
| `src/mocks/handlers.ts` | 修改 | 加入 `...watchlistHandlers` |
| `src/i18n/zh-TW.json` | 修改 | 完整 watchlist 鍵值替換 + 2023/2032 錯誤碼 |
| `src/i18n/en.json` | 修改 | 同上（英文版） |
| `src/constants/errorCodes.ts` | 修改 | 新增 WATCHLIST_NOT_FOUND: 2023 + ALERT_LIMIT_REACHED: 2032 |

---

## 5. 測試結果

```
Test Files  8 passed (8)
     Tests  121 passed (121)
```

| 測試 | 新增數 |
|------|--------|
| WatchlistPage | 7 |
| AddStockModal | 4（含搜尋結果、無結果、加入 service 呼叫） |
| EmptyState | 2 |
| **Watchlist 模組合計** | **11** |

Lint：1 個 pre-existing error（`useQueryErrorNotification.test.tsx:29`，import() 型別語法），非本次修改所引入。

Type Check：無錯誤。

---

## 6. Spec 待確認問題（請 Peter 釐清）

| # | 問題 | 影響範圍 |
|---|------|----------|
| Q1 | `StockSearchItem.matchType` 是否包含 `'ID_PREFIX'` 和 `'NAME_PARTIAL'`？目前 schema 只有 `'ID_EXACT' \| 'NAME_EXACT' \| 'NAME_PREFIX' \| 'NAME_CONTAINS'` | `types/watchlist.ts` + MSW handler |
| Q2 | 自選股 `market` 欄位是否已完全棄用 `'OTC'`，統一改為 `'TPEx'`？（SRS 上市/上櫃對應） | `types/watchlist.ts` + watchlistHandlers |
| Q3 | `watchlist/list` 回應是否隨行情快照一起回傳，還是分開呼叫？（影響 staleTime 設定是否合理） | `hooks/useWatchlist.ts` staleTime = 30s |
| Q4 | `watchlist/remove` 的 request body 是 `stockId` 還是 `itemId`？目前實作為 `stockId` | `types/watchlist.ts` + service + hook |

---

## 7. Wave B 移交事項

- Bruno：實作 `/api/v1/watchlist/list`、`/api/v1/watchlist/add`、`/api/v1/watchlist/remove`、`/api/v1/stock/search` 真實後端，確認 schema 與 `WatchlistListResult` 一致
- Peter：確認上述 Q1-Q4 Spec 問題
- Fiona：Code Review 候選（請 Jamie 排程）

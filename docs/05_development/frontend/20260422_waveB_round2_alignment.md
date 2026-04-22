# Wave B Round 2 跨團隊 DTO 對齊筆記

| 項目 | 內容 |
|------|------|
| 日期 | 2026-04-22 |
| 作者 | Felix（資深前端工程師） |
| 範圍 | Wave B Round 2：DTO/errorCode 跨團隊對齊（依 Peter schema-lock + Jamie 仲裁 D1-D6） |
| 依據 | `docs/03_spec/20260422_schema-lock_stock-detail-apis.md`（v1.0 LOCKED） |
| 依據 | `docs/03_spec/20260422_errorCodes_central.md`（v1.1 LOCKED） |
| 前置 | Round 1 修補（`20260422_waveB_round1_fix.md`）已完成，50/50 tests 全綠 |

---

## 1. 對齊原則

- 前端 DTO 形式被採納（Felix-led）：後端 Bruno 跟著改欄位名
- 錯誤碼以後端段落為主（Backend-led）：前端廢除 5101，採用 5010 系列
- 一切以 Peter schema-lock 為唯一事實源，不自行決定細節

---

## 2. 修改檔案清單

### A. errorCodes.ts 對齊

**檔案**：`frontend/src/constants/errorCodes.ts`

| 動作 | 說明 |
|------|------|
| 移除 `TWSE_UNAVAILABLE: 5101` | Jamie 仲裁 D4 廢除，central 已 deprecated |
| 新增 `TWSE_DATA_SOURCE_ERROR: 5010` | central §5xxx 正式常數 |
| 新增 `MOPS_DATA_SOURCE_ERROR: 5011` | Wave B 新增 |
| 新增 `CHIP_DATA_SOURCE_ERROR: 5012` | Wave B 新增 |
| 新增 `OTC_DATA_SOURCE_ERROR: 5013` | Wave B 新增（Brian M-BE-W2-09） |
| 新增 `MOPS_FORMAT_CHANGED: 5014` | Wave B 新增（Brian M-BE-W2-08） |
| 新增 `FORBIDDEN: 3004` | 已登入但無此功能權限 |
| 新增 `FEATURE_NOT_AVAILABLE: 9003` | Placeholder API 回傳 |
| 新增 `isDataSourceError()` 輔助函式 | 5010-5014 範圍判斷 |
| 修正 `PARAM_MISSING → PARAM_REQUIRED` | 對齊 central 1001 Constant 名稱 |
| 修正 `PARAM_FORMAT → PARAM_FORMAT_INVALID` | 對齊 central 1002 Constant 名稱 |
| 補齊完整 2xxx / 4xxx / 9xxx 段落 | 對齊 central 清單 |

**連帶修正**：
- `frontend/src/services/http.ts`：`ErrorCode.UNKNOWN` → `ErrorCode.UNKNOWN_ERROR`
- `frontend/src/services/http.test.ts`：`ErrorCode.PASSWORD_INCORRECT` → `ErrorCode.EMAIL_OR_PASSWORD_INCORRECT`

### B. types/stock.ts 對齊 schema-lock

**檔案**：`frontend/src/types/stock.ts`

| 型別 | 變更 |
|------|------|
| `StockQuote` | 補 `market / previousClose / open / high / low / quoteDate / isStale / source`（schema-lock §6） |
| `KLineItem`（新增） | 取代舊 `StockHistoryItem`（名稱對齊 schema-lock §6） |
| `KLineHistory`（新增） | 包裝物件 `{ stockId, period, items[] }` |
| `StockFundamental`（新增） | 欄位名 `per/pbr`（非 `perRatio/pbrRatio`），補 `reportYear / reportQuarter / source: 'MOPS'` |
| `InstitutionItem`（新增） | 取代舊 `ChipInstitutionEntry`，`name` 改為 `InstitutionName` union type |
| `StockChip`（新增） | 補 `totalNetBuySell / source`（schema-lock §5.3） |
| 向下相容別名 | `StockHistoryItem = KLineItem`、`Fundamental = StockFundamental`、`Chip = StockChip`、`ChipInstitutionEntry = InstitutionItem` |

### C. stockService.ts 對齊

**檔案**：`frontend/src/services/stockService.ts`

| 變更 | 說明 |
|------|------|
| `getHistory()` 回傳改為 `KLineHistory` | 包裝物件，含 `stockId / period / items[]` |
| `getHistory()` 新增 `startDate? / endDate?` 選填參數 | schema-lock §3.2 |
| 型別更新 | `StockFundamental / StockChip`（新規範名稱） |
| 移除舊版 `{ stockId, month }` 對應 | D3 仲裁廢除 |

### D. MSW handlers.ts 對齊

**檔案**：`frontend/src/mocks/handlers.ts`

| 變更 | 說明 |
|------|------|
| quote mock | 補 `previousClose / market / open / high / low / quoteDate / isStale / source` |
| fundamental mock | `per/pbr`（非 `perRatio/pbrRatio`）、補 `stockName / reportYear / reportQuarter`、`source: 'MOPS'`、`updatedAt` 改為 ISO 8601 datetime |
| chip mock | 補 `totalNetBuySell / source`，`institutions[]` 每筆含 `buy / sell / netBuySell` |
| history mock | 改為 `KLineHistory` 包裝物件，解析 request body `period` 動態回傳 daily/weekly/monthly |
| 新增 `errorHandlers` | 提供 5010 / 5011 / 5012 / isStale 情境，供測試使用 |
| buildEnvelope timestamp | 改為 `+08:00` 格式（schema-lock §0 時區規範） |

### E. 元件繫結更新

| 元件 | 變更 |
|------|------|
| `PriceHeader.tsx` | 補 `market` Tag、`previousClose` 顯示；型別改 `StockQuote`（含新欄位） |
| `KLineChart.tsx` | `StockHistoryItem` → `KLineItem`（別名，型別兼容無破壞性變更） |
| `FundamentalCard.tsx` | 型別改 `StockFundamental`、補 `reportYear/reportQuarter` 顯示、移除 null 處理（schema-lock 所有欄位必填） |
| `ChipCard.tsx` | 型別改 `InstitutionItem / StockChip`、補 `totalNetBuySell` 顯示區塊（`data-testid="chip-total-net"`） |
| `StockDetail/index.tsx` | `historyQuery.data?.items` 傳給 KLineChart；新增 `isStale` 警示 Tag（`stale-data-tag`）；4001 改用 `ErrorCode.STOCK_NOT_FOUND` |

### F. useStockHistory.ts 回傳型別更新

**檔案**：`frontend/src/pages/StockDetail/hooks/useStockHistory.ts`

`useQuery<StockHistoryItem[], Error>` → `useQuery<KLineHistory, Error>`

### G. i18n 補齊

**檔案**：`frontend/src/i18n/zh-TW.json`、`frontend/src/i18n/en.json`

- 移除舊 `"5101"` key（廢除）
- 補齊 5010 / 5011 / 5012 / 5013 / 5014 對應 i18n messages
- 補齊完整 2xxx / 4xxx / 9xxx i18n messages
- 新增 `stock.previousClose`、`stock.staleData`（含 `{{date}}` interpolation）
- 新增 `stock.chip.totalNetBuySell`
- 新增 `stock.fundamental.reportPeriod`

---

## 3. 測試變更

**檔案**：`frontend/src/pages/StockDetail/StockDetail.test.tsx`

| 項目 | 說明 |
|------|------|
| 所有 mock data 補齊 schema-lock 新欄位 | `happyQuote / happyFundamental / happyChip / happyHistory` 均對齊 schema-lock |
| `staleQuote` mock | 新增 `isStale: true` 情境 |
| 新增 isStale=true 顯示警示 Tag 測試 | `stale-data-tag` testId 出現 |
| 新增 isStale=false 不顯示 Tag 測試 | 確保無誤報 |
| 新增 5010 TWSE_DATA_SOURCE_ERROR 整合測試 | quote 失敗不跳 stock-not-found |
| 新增 5011 MOPS_DATA_SOURCE_ERROR 整合測試 | fundamental 失敗不影響 quote |
| 新增 5012 CHIP_DATA_SOURCE_ERROR 整合測試 | chip 失敗不影響 quote |
| useStockHistory hook 測試 | 驗證回傳 `KLineHistory.items` 與 `period` |
| useFundamental hook 測試 | 驗證 `per/pbr`（非 perRatio/pbrRatio）、`reportYear/Quarter/source` |
| useChip hook 測試 | 驗證 `totalNetBuySell / source`；error code 改 5012 |
| ChipCard totalNetBuySell 顯示測試 | `chip-total-net` testId |
| PriceHeader market 顯示測試 | `price-market` testId |
| PriceHeader previousClose 顯示測試 | 文字內容驗證 |
| 4001 改用 `ErrorCode.STOCK_NOT_FOUND` | 取代舊版 `RESOURCE_NOT_FOUND` |

測試數量：50（Round 1） → 59（Round 2 新增 9 個）

---

## 4. 最終機械檢查結果

| 檢查項目 | 結果 |
|----------|------|
| type-check | 0 errors |
| lint | 0 warnings |
| vitest | 59/59 passed（新增 9 個測試） |
| 5101 殘留檢查 | 0 處（已完全廢除） |
| any / ts-ignore / non-null assertion | 0 |

---

## 5. 技術決策補充

### 向下相容別名設計

`StockHistoryItem / Fundamental / Chip / ChipInstitutionEntry` 以 `type` 別名保留，避免牽動大量未在本輪對齊範圍內的測試檔與元件。未來 Wave C 可直接統一改為 `KLineItem / StockFundamental / StockChip / InstitutionItem`。

### KLineHistory 包裝物件

`getHistory()` 由直接回傳 `StockHistoryItem[]` 改為 `KLineHistory`（含 `stockId / period / items[]`）。原因：
1. 後端 schema-lock §3.3 已明確定義包裝物件
2. `period` echo back 讓前端可以驗證請求/回應一致性
3. `stockId` echo back 利於未來批次或 cache 識別

StockDetail/index.tsx 傳 `historyQuery.data?.items` 給 KLineChart，元件介面不變（仍消費 `KLineItem[]`）。

### isStale 警示 Tag 位置

警示 Tag 置於 StockDetail 頁面最頂端（PriceHeader 之上），而非嵌入 PriceHeader 內部，理由：
- PriceHeader 職責只顯示行情數字，不做業務層狀態判斷
- 未來若其他資料源（fundamental/chip）也有 isStale 概念，可在此統一顯示

---

## 6. 待 Fiona Round 2 Review 重點

| 項目 | 重點 |
|------|------|
| errorCodes.ts | 與 central v1.1 完整對比，確認無遺漏、無多餘 |
| types/stock.ts | StockQuote / StockFundamental / StockChip / KLineHistory 欄位對比 schema-lock §6 |
| stockService.ts | getHistory() 回傳型別、request body 無舊版 `month` 欄位 |
| handlers.ts | mock response 100% 對齊 schema-lock，errorHandlers 可供測試使用 |
| StockDetail/index.tsx | isStale Tag 顯示邏輯、historyQuery.data?.items 傳遞 |
| FundamentalCard.tsx | 無 null 處理（schema-lock 所有欄位必填） |
| ChipCard.tsx | totalNetBuySell 顯示、型別使用 InstitutionItem |
| i18n | 5101 key 移除確認；5010-5014 messages 完整 |
| 測試 | 新增 9 個：isStale + 5010/5011/5012 + 新欄位驗證 |
| http.ts / http.test.ts | `UNKNOWN_ERROR` 與 `EMAIL_OR_PASSWORD_INCORRECT` 常數名修正 |

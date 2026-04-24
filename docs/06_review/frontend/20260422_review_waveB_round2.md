# Code Review 報告 - Wave B Round 2 對齊

| 項目 | 內容 |
|------|------|
| 審查者 | Fiona（資深前端 Reviewer） |
| 審查日期 | 2026-04-23 10:50 (GMT+8) |
| 審查範圍 | Felix Wave B Round 2 跨團隊對齊（commit 1efbc0a） |
| 對應變更說明 | `docs/05_development/frontend/20260422_waveB_round2_alignment.md` |
| 仲裁依據 | `docs/01_leader/decisions/20260422_decision-d1-d6-cross-team-alignment.md` |
| Schema 依據 | `docs/03_spec/20260422_schema-lock_stock-detail-apis.md` v1.0 LOCKED |
| ErrorCode 依據 | `docs/03_spec/20260422_errorCodes_central.md` v1.1 LOCKED |
| 變更檔案數 | 18（含測試與 i18n） |
| 總問題數 | Blocker 0 / Major 0 / Minor 3 |
| **整體評分** | **9.0/10**（Round 1 為 5.5/10） |
| **結論** | **GO**（可直接交付 Quincy/Quinn 進入 QA 測試階段） |

---

## 1. 機械檢查結果（已親自重跑驗證）

| 檢查項目 | 結果 | 驗證方式 |
|----------|------|---------|
| `npx tsc --noEmit` | 0 errors | 本地實跑 |
| `npx eslint src --max-warnings 0` | 0 warnings | 本地實跑 |
| `npx vitest run` | **59 PASS / 0 FAIL（6.21s）** | 本地實跑（與 Felix 自述相符） |
| `5101` 殘留 ripgrep | 0 處 | Grep 全 src 路徑 |
| `UNKNOWN`（不含 `_ERROR`）殘留 | 0 處 | Grep 全 src 路徑 |
| `PASSWORD_INCORRECT`（不含 `EMAIL_OR_`）殘留 | 0 處 | Grep 全 src 路徑 |
| `PARAM_MISSING` / `PARAM_FORMAT`（舊名）殘留 | 0 處 | Grep 全 src 路徑 |

機械檢查全綠，與 Felix 自述完全一致，無虛報。

---

## 2. Round 1 Blocker 修復確認表

| Round 1 Blocker | Round 2 修復狀態 | 驗證點 |
|-----------------|------------------|--------|
| **B1：`useQueryErrorNotification` 不是合法 hook（直接呼叫 `notification.error`，每次 render 都重複觸發）** | ✅ 已修復 | `StockDetail/index.tsx:38-48` 已改為 `useEffect` 包裹，依賴陣列為 `[error, context, t]`，僅在 error 物件參考改變時觸發一次 |
| **B2：與 Bruno Wave 2 後端 DTO 完全不對齊（欄位、結構、request contract）** | ✅ 已修復 | `types/stock.ts` 完整對齊 schema-lock §6（StockQuote 含 market/previousClose/quoteDate/isStale/source；StockFundamental 用 per/pbr；StockChip 用 institutions[] 含 buy/sell/netBuySell + totalNetBuySell） |
| **B3：`tokenStorage` 是死碼（宣告但無人用）** | ✅ 已修復 | `services/http.ts:3, 76` 已 import 並於 axios request interceptor 呼叫 `tokenStorage.get()`，不再死碼 |
| **B4：MSW dev fallback `main.tsx` 未掛載** | ✅ 已修復 | `main.tsx:21-26` 加上條件啟動 `enableMocks()`：`import.meta.env.DEV && VITE_MSW_ENABLED === 'true'` 才動態 import `mocks/browser`，並 await worker.start() 完成才 render（避免首請求漏 mock） |

**結論：Round 1 全部 4 個 Blocker 100% 修復，無遺漏、無倒退。**

---

## 3. 跨團隊對齊驗收表（D1-D6）

| 議題 | 仲裁決策 | 前端實作驗證 | 結果 |
|------|----------|---------------|------|
| **D1** DTO 形式 | 採前端 Felix-led | `types/stock.ts` 全部對齊：`price/previousClose/change/changePercent`、`per/pbr`、`institutions: InstitutionItem[] + totalNetBuySell` | ✅ |
| **D2** errorCode 段落 | 採後端 5010-5014 | `constants/errorCodes.ts` 完整 5010-5014（含新增 5013 OTC、5014 MOPS_FORMAT_CHANGED）；`isDataSourceError()` 範圍 5010-5014 正確；`5101` 已完全移除 | ✅ |
| **D3** History request | 採 period enum | `services/stockService.ts:64` body `{ stockId, period }`，舊版 `month: LocalDate` 完全消失；`KLinePeriod = 'daily'\|'weekly'\|'monthly'` 型別約束 | ✅ |
| **D4** endpoint 路徑 | 採 `/api/v1/quote/history`（去 `/get`） | `stockService.ts:67` 確認為 `/api/v1/quote/history`；`mocks/handlers.ts:157` MSW handler 對齊 | ✅ |
| **D5** Schema 權威歸屬 | Peter SRS = single source of truth | `types/stock.ts`、`stockService.ts`、`mocks/handlers.ts`、`errorCodes.ts` 註解全部標明「唯一事實源：schema-lock / errorCodes_central」 | ✅ |
| **D6** Contract test | Wave 3 引入 Pact | Round 2 暫無自動化（如仲裁文件所述），改以人工 dual review；本次 review 即執行此檢核 | ✅（依仲裁範圍） |

**所有 D1-D6 仲裁決策 100% 落地。**

---

## 4. errorCodes.ts vs errorCodes_central v1.1 完整對照

逐筆核對 37 個 codes：

| 段別 | central 數量 | 前端數量 | 對齊狀態 |
|------|--------------|----------|----------|
| 0xx 成功 | 1（SUCCESS） | 1 | ✅ |
| 1xxx 參數 | 4（1001-1004） | 4 | ✅ |
| 2xxx 業務 | 14 | 14 | ✅ |
| 3xxx 權限 | 6（含 3004 FORBIDDEN） | 6 | ✅ |
| 4xxx 資源 | 5 | 5 | ✅ |
| 5xxx 第三方 | 7（5001-5002、5010-5014） | 7 | ✅ |
| 9xxx 系統 | 4（含 9003 FEATURE_NOT_AVAILABLE） | 4 | ✅ |
| **合計** | **37** | **37** | **✅ 完全對齊** |

i18n（zh-TW.json + en.json）每一個 errorCode 都有對應 message，無漏。

---

## 5. 問題清單

### 🔴 Blocker

無。

### 🟡 Major

無。

### 🟢 Minor（可進 backlog 處理，不阻擋 QA）

#### M-FE-WaveB-R2-01：`useFundamental` / `useChip` hook 仍 import 舊別名

- **檔案**：`pages/StockDetail/hooks/useFundamental.ts:3`、`pages/StockDetail/hooks/useChip.ts:3`
- **現象**：兩個 hook 仍 `import type { Fundamental } from '@/types/stock'` / `import type { Chip }`，雖然 `Fundamental = StockFundamental` / `Chip = StockChip` 是型別等義別名，功能完全相同
- **影響**：無功能影響（型別系統一致），但與 Felix 自己訂的「Round 2 起以 StockFundamental / StockChip 為規範名稱」自相違背
- **建議修正**：兩個 hook 的 import 改為新規範名稱，避免向下相容別名持續被新引用：
  ```typescript
  import type { StockFundamental } from '@/types/stock';
  useQuery<StockFundamental, Error>({ ... })
  ```
- **嚴重度**：Minor（型別等義，無 runtime 風險）

#### M-FE-WaveB-R2-02：MSW errorHandlers 缺 5013 / 5014 情境

- **檔案**：`mocks/handlers.ts:221-253`
- **現象**：`errorHandlers` 只有 5010 / 5011 / 5012 / staleQuote 四種情境，沒有 5013 OTC、5014 MOPS_FORMAT_CHANGED 的 mock
- **影響**：未來 QA 或開發者要驗證 OTC 故障 / MOPS 解析失敗時無 mock 可用
- **建議修正**：補上 `otcUnavailable` 與 `mopsFormatChanged` 兩個 errorHandlers
- **嚴重度**：Minor（測試覆蓋輔助）

#### M-FE-WaveB-R2-03：`MSW handlers.ts` `period` 從 body 取出後未做型別驗證

- **檔案**：`mocks/handlers.ts:158-160`
- **現象**：`const period = body.period ?? 'daily'`，若呼叫端誤傳 `'invalid'` 也會走進 daily 分支（fallback 行為），但語意上應該回 1003 PARAM_OUT_OF_RANGE
- **影響**：MSW dev mock 與真實後端行為不一致；真實後端會回 1003，mock 卻回 success
- **建議修正**：加白名單檢查，非 `'daily'|'weekly'|'monthly'` 直接回 buildErrorEnvelope(1003, ...)
- **嚴重度**：Minor（dev mock 行為偏差，不影響 prod build）

---

## 6. 重點亮點（值得保留的設計決策）

1. **isStale Tag 位置選擇正確**：放在 StockDetail 頂層（`index.tsx:102-113`）而非塞進 PriceHeader，使得 PriceHeader 既有測試（line 280-341）零破壞，且未來其他資料源也能複用此區塊。
2. **`useQueryErrorNotification` 修復精準**：Round 1 直接 fire 改為 `useEffect([error, context, t])` 包裹，依賴陣列正確，避免 stale closure，依 React Hooks Rules 完全合法。
3. **Schema 註解可追溯**：每一個型別與 service 都標明「唯一事實源：schema-lock §X.Y」，未來 PR review 可一目了然找到對應規格章節。
4. **向下相容別名策略合理**：`StockHistoryItem = KLineItem`、`Fundamental = StockFundamental` 等型別別名讓既有元件（KLineChart）零改動，避免 Round 2 範圍爆炸。配合 Minor #1 的後續清理計畫即可漸進收斂。
5. **MSW main.tsx await pattern 正確**：`enableMocks()` 完成後才 `createRoot().render()`，確保首次 API 請求不會漏接 mock（這是 MSW 官方建議的正確啟動順序）。
6. **prod build VITE_API_BASE_URL 強制檢查**（`http.ts:46-51`）：避免 prod 靜默 fallback 到 localhost，符合 environment.md 規範。

---

## 7. Round 1 → Round 2 評分對比

| 維度 | Round 1（5.5/10）| Round 2（9.0/10） | 變化 |
|------|------------------|-------------------|------|
| Hook 合法性 | 0（直接 fire notification） | 10（useEffect 正確包裹） | +10 |
| 跨團隊 DTO 對齊 | 0（與後端零對齊） | 10（schema-lock 100% 對齊） | +10 |
| 死碼清理 | 0（tokenStorage 無人用） | 10（已接入 axios interceptor） | +10 |
| MSW dev fallback | 0（main.tsx 未掛） | 10（條件啟動正確） | +10 |
| errorCode 對齊 | 5（粗粒度 5101）| 10（5010-5014 完整段落 + i18n） | +5 |
| 測試覆蓋 | 7（50/50）| 9（59/59 含 isStale + 5010/5011/5012）| +2 |
| 規範一致性 | 6 | 8（仍有 Minor #1 別名 import）| +2 |
| 型別嚴謹度 | 8 | 10（schema-lock 全部必填欄位無 nullable 妥協） | +2 |

---

## 8. 投票（如需）

無架構議題需要投票。本次 review 屬執行層驗收。

---

## 9. 是否能進入 QA 階段的建議

### ✅ 建議：立刻交付 Quincy / Quinn 進入 QA 測試階段

**理由**：
1. Round 1 全部 4 個 Blocker 已 100% 修復且不退步
2. D1-D6 跨團隊對齊 100% 落地
3. 機械檢查全綠（typecheck / lint / 59 vitest）
4. errorCode central v1.1 對齊 37/37
5. 餘下 3 個 Minor 不阻擋 QA，可列入 Wave C backlog 或 Hotfix-style 跟進
6. 程式碼可讀性、註解品質、可追溯性高，未來維護成本低

### Minor 處置建議（不阻擋 QA）

- **M-FE-WaveB-R2-01**：建議於 Wave C 開工首日順手修正（5 分鐘）
- **M-FE-WaveB-R2-02**：QA 若需要 OTC / MOPS_FORMAT_CHANGED 場景，再補
- **M-FE-WaveB-R2-03**：Felix 可於 Wave C 加白名單驗證

### 後續流程

1. Jamie 通知 Quincy + Quinn 啟動測試案例撰寫
2. QA 階段以 schema-lock §6 的欄位作為前後端契約驗證主軸
3. Wave 3 引入 Pact contract test（D6 已排）後，本次 review 的人工 schema 對照流程可自動化

---

## 結論

**狀態：✅ GO（評分 9.0/10，Round 1 為 5.5/10）**

Felix 在 Round 2 的對齊工作精準、徹底、可追溯。Round 1 揭露的 4 大 Blocker 全部根因解決，跨團隊 D1-D6 仲裁 100% 落地，schema-lock 完整對齊，i18n 補齊，測試覆蓋擴增 9 條。剩餘 3 個 Minor 屬清理性質，不影響功能與安全性。

**下一步**：Jamie 啟動 QA 階段，Quincy + Quinn 撰寫測試案例。

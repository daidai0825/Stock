# Wave 2 Spec Clarifications

| 項目 | 內容 |
|------|------|
| 文件版本 | v1.0 |
| 撰寫者 | Peter（產品經理） |
| 撰寫日期 | 2026-04-23 |
| 狀態 | **FINAL（正式決策）** |
| 觸發來源 | QA Cross-review（Quincy + Quinn）+ 開發 Triage（Bruno + Felix） |
| 參考依據 | `docs/05_development/backend/20260423_bruno_qa_defect_triage.md`、`docs/05_development/frontend/20260423_felix_qa_defect_triage.md`、`docs/03_spec/20260422_schema-lock_stock-detail-apis.md` v1.0 |
| 下游使用者 | Bruno（後端修復）、Felix（前端修復）、Brian（後端 Review）、Fiona（前端 Review）、Quincy / Quinn（測試案例更新） |

> 本文件是 `20260422_schema-lock_stock-detail-apis.md` v1.0 的**補充澄清**，不取代原文件。
> 原文件維持 LOCKED 狀態，本文件作為附件生效。
> 任何依本文件執行的程式碼修改，PR 標題需加 `[SRS-CHANGE]` 前綴。

---

## Decision #1：ChipDTO stockName 欄位

### 問題描述

後端 `ChipDTO.java` record 含 `stockName` 欄位，`ChipConvertor.java` 也確實將 `po.getStockName()` 傳入，導致 `POST /api/v1/chip/get` 的 JSON response 多出 `stockName` 欄位。然而 schema-lock §5.3 定義的 `StockChip` TypeScript 介面不含此欄位，形成 schema contract 不一致。

### 決策：方案 B（後端移除 stockName，schema-lock 不動）

**理由：**

1. **schema-lock 是正確的**。`/api/v1/chip/get` 端點的設計目的是提供「三大法人買賣超資料」，股票名稱並非此端點的職責範圍。`stockName` 已在 `/api/v1/fundamental/get`（schema-lock §4.3）與 `/api/v1/quote/get`（schema-lock §1.3）中完整提供，前端在頁面層面已能取得股票名稱，無需 `/chip/get` 重複回傳。
2. **方案 A 的問題**：若在 `StockChip` 加入 `stockName`，等同為了修正後端實作疏失而讓 schema 膨脹，形成「多一個沒有使用意義的欄位」，增加未來 consumer 的理解成本與 Pact contract test 的維護負擔。
3. **修復成本極低（S）**：移除 DTO 欄位 + 調整 Convertor + 更新測試輔助方法，Bruno 預估 0.5h，無業務邏輯影響。
4. **Wave 3 Pact contract test 前必須修復**：否則 TC-CONTRACT-003 在 contract test 啟用後立即 FAIL。

### 執行 Action

| 項目 | 負責人 | 時間點 | 說明 |
|------|--------|--------|------|
| `ChipDTO.java` 移除 `stockName` 欄位 | Bruno | Wave 3 Sprint 開始前 | 不 block Wave 2 release |
| `ChipConvertor.java` 移除 `po.getStockName()` 傳入 | Bruno | Wave 3 Sprint 開始前 | 同上 |
| `ChipServiceImplTest.java` 更新 `buildChipDTO()` 輔助方法 | Bruno | Wave 3 Sprint 開始前 | 移除 stockName 引數 |
| `20260422_schema-lock_stock-detail-apis.md` 無需修改 | - | - | schema-lock §5.3 原定義正確 |
| Wave 3 Pact contract test 驗收 | Quincy | Wave 3 QA 階段 | TC-CONTRACT-003 |

### schema-lock 補充說明（適用 §7.3 ChipDTO 章節）

> **補充（2026-04-23）**：ChipDTO Wave 2 實作中誤含 `stockName` 欄位（非 schema-lock §5.3 規範欄位），已確認為實作疏失（BUG-QUINCY-001）。決策：後端移除此欄位，schema-lock §5.3 維持原定義。Wave 3 Sprint 開始前須完成修復，以確保 Pact contract test 啟用後不 FAIL。

---

## Decision #2：`isStale=true` 時 quoteDate 的語意

### 問題描述

schema-lock §1.3 的設計說明僅寫：

> `isStale: true` 時，前端應顯示「資料延遲，最後更新：{quoteDate}」之類的提示文案。

未明確定義 `quoteDate` 在 `isStale=true` 時的確切語意：是「API 被呼叫的當日」還是「fallback DB 那筆資料的實際交易日」。

### 決策：方案 A（保持現行實作語意）

**`isStale=true` 時，`quoteDate` 為 fallback DB 最後一筆資料的實際交易日，非 API 請求當日。**

例：週一 (2026-04-20) 上午呼叫 API，TWSE 即時資料失敗，fallback 到 DB 最後一筆（2026-04-19，週五收盤資料），則回傳 `quoteDate: "2026-04-19"`，不回傳請求當日 `"2026-04-20"`。

**理由：**

1. **語意對使用者最有意義**：前端顯示「資料延遲，最後更新：2026-04-19」，使用者可清楚知道目前看到的是哪一天的資料，而非今天嘗試取得資料失敗的事實（使用者對「今天失敗」無感，對「資料是幾號的」有感）。
2. **後端現行實作已使用此語意**：`QuoteServiceImpl.java` 兩種 isStale=true 情境（L136-147、L157-165）均取 `po.getQuoteDate()`（DB 那筆的交易日），不需改 code。
3. **MSW mock 已對齊**：`staleQuote` mock 使用前一個交易日（2026-04-19）作為 quoteDate，與後端行為一致，測試案例基準正確。
4. **方案 B（請求當日）的問題**：`quoteDate` 應反映資料的日期，若填入請求當日，前端顯示「最後更新：今日」但資料實際是昨天的，語意矛盾，且誤導使用者。
5. **方案 C（雙欄位）**：資訊最完整，但 Wave 2 修改 schema 影響範圍大，Wave 3 有需求再評估。

### schema-lock §1.3 補充段落（直接補充至設計說明）

在 schema-lock §1.3「設計說明」第二條後，補充以下文字：

---

> **`quoteDate` 在 `isStale=true` 時的語意（補充，2026-04-23）**：
>
> `isStale=true` 時，`quoteDate` 為 fallback DB 最後一筆資料的**實際交易日**，非 API 請求當日。
>
> 範例：
> - 請求日：2026-04-20（週一）
> - TWSE 即時資料不可用，fallback 至 DB 最後一筆（2026-04-19，週五）
> - 回傳：`quoteDate: "2026-04-19"`，`isStale: true`
>
> 前端應以 `quoteDate` 的值顯示「資料延遲，最後更新：{quoteDate}」，不得顯示請求當日日期。
>
> 此語意適用於以下兩種 isStale=true 情境：
> 1. TWSE/OTC 外部資料取得成功，但 DB 資料已超過 1 個工作日（非今日）
> 2. TWSE/OTC 外部資料取得失敗（週末 / 假日 / 來源不可用），fallback 至 DB 最後一筆

---

### 執行 Action

| 項目 | 負責人 | 時間點 | 說明 |
|------|--------|--------|------|
| `20260422_schema-lock_stock-detail-apis.md` §1.3 補充說明 | Bruno（或 Jamie 指定人員更新文件） | Wave 2 release 前 | 補充上方段落至設計說明，不改欄位定義 |
| `QuoteServiceImpl.java` 不需修改 | - | - | 現行實作語意正確 |
| TC-QUOTE-007 測試基準確認 | Quincy | Wave 2 QA | 預期值為 fallback DB 那筆的 quoteDate（如 2026-04-19），非請求當日，與現行 mock 一致 |

---

## Decision #3：5014 MOPS_FORMAT_CHANGED 的 UI 行為

### 問題描述

Felix triage 確認兩個問題：

**根本問題（範圍更廣）**：`useQueryErrorNotification` 的 `message` 欄位直接使用後端回傳的英文字串（`error.message`），導致所有 5010-5014 錯誤通知均顯示後端英文，完全沒有使用前端 i18n 翻譯。

**語意問題（SPEC-Q-002 原始問題）**：5014 `MOPS_FORMAT_CHANGED` 是「結構性錯誤」（MOPS parser 需更新，重試無法解決），與 5010-5013「暫時性服務不可用」（重試可能解決）語意不同，但 UI 行為目前無區分。

### 決策：方案 B（新增 `isFatalDataSourceError`，5014 走特定分支）

採用 Felix 建議的方案 B，並確認以下細節。

#### 3.1 notification type

**維持 `notification.error`（紅色）**，不改用 `notification.warning`（橘色）。

理由：5014 是一個需要立即工程師介入的錯誤，紅色傳達「有問題需要處理」的緊迫性，此與 `notification.warning`「注意但可繼續」的語意不符。區分方式改用 description 文字而非 notification type 顏色。

#### 3.2 i18n 文案（正式確認）

**message（顯示錯誤標題，使用 i18n 翻譯）**：

| key | zh-TW 文案 | en 文案 |
|-----|-----------|---------|
| `errors.5014` | `MOPS 資料格式異動，請聯絡系統管理員` | `MOPS response format has changed; please contact system administrator` |

> 注意：zh-TW.json 已有 `errors.5014` key，文案維持不變。en.json 需確認是否已存在，若無則新增。

**description（區分 fatal vs. recoverable）**：

新增以下 i18n key：

| key | zh-TW 文案 | en 文案 |
|-----|-----------|---------|
| `errors.fatalDataSourceDesc` | `此錯誤需後端工程師處理，無法透過重試解決。traceId：{{traceId}}` | `This error requires backend engineer attention and cannot be resolved by retrying. traceId: {{traceId}}` |

5010-5013（可恢復錯誤）的 description 維持現行邏輯（顯示 `traceId：xxx`），不額外說明「重試」，避免與新的 fatal description 混淆。

#### 3.3 errorCodes.ts 新增函式

新增 `isFatalDataSourceError` 函式，**初始版本只含 5014**，設計上預留擴充空間（陣列實作）：

```typescript
// 結構性資料來源錯誤（重試無法解決，需工程師介入）
// 目前含：5014 MOPS_FORMAT_CHANGED
// 未來可能擴充：5024 OTC_FORMAT_CHANGED、5034 FUNDAMENTAL_PARSER_ERROR 等
const FATAL_DATA_SOURCE_ERROR_CODES: ReadonlySet<number> = new Set([
  ErrorCode.MOPS_FORMAT_CHANGED,  // 5014
]);

export const isFatalDataSourceError = (code: number): boolean =>
  FATAL_DATA_SOURCE_ERROR_CODES.has(code);
```

#### 3.4 未來擴充範圍

`isFatalDataSourceError` 的擴充原則：**凡是「parser / 格式解析失敗」類錯誤（即重試任意次數都無法解決、必須工程師修改程式才能恢復）的 error code，均應納入此集合**。

目前確認範圍：

| code | Constant | 說明 | 是否已納入 |
|------|----------|------|-----------|
| 5014 | MOPS_FORMAT_CHANGED | MOPS API 格式異動，parser 需更新 | **是（Wave 2）** |
| 5024 | 未定義 | 預留：OTC 格式異動（如未來有此需求） | 否（Wave 3+ 視需求新增） |
| 5034 | 未定義 | 預留：其他資料來源格式異動 | 否（Wave 3+ 視需求新增） |

> **命名原則**：5x4 結尾的 code 保留給「格式異動 / parser 失效」類錯誤。若 Wave 3+ 新增類似錯誤，優先考慮此命名規律並提案更新本文件。

### 執行 Action

| 項目 | 負責人 | 時間點 | Block Wave 2？ | 說明 |
|------|--------|--------|---------------|------|
| `useQueryErrorNotification` message 改用 i18n（`t('errors.{code}', { defaultValue: error.message })`） | Felix | Wave 2 release 前 | **Yes（TC-Q-004 為 P0）** | 所有 5xxx 錯誤的根本修復 |
| `errorCodes.ts` 新增 `isFatalDataSourceError`（含 5014） | Felix | Wave 2 release 前 | Yes | 與上項同批 PR |
| `useQueryErrorNotification` 加入 fatal 分支：5014 走 `errors.fatalDataSourceDesc` description | Felix | Wave 2 release 前 | Yes | 對應 SPEC-Q-002 |
| `zh-TW.json` 新增 `errors.fatalDataSourceDesc` key | Felix | Wave 2 release 前 | Yes | 文案見 §3.2 |
| `en.json` 確認 / 新增 `errors.5014` 和 `errors.fatalDataSourceDesc` key | Felix | Wave 2 release 前 | Yes | 文案見 §3.2 |
| StockDetail 單元測試更新（5014 走 fatal 分支驗證） | Felix | Wave 2 release 前 | Yes | 預計 1 小時 |
| TC-Q-004-05 驗收基準更新（message 改驗 i18n 翻譯） | Quinn | Wave 2 QA | Yes | 預期 message 為 zh-TW `errors.5014` 內容 |
| TC-Q-005-02 驗收基準更新（en 語言 5014 驗證） | Quinn | Wave 2 QA | 視 BUG-Q-001 修復時間 | 依賴 Felix BUG-Q-001 i18n 修復 |
| i18n 不讀 localStorage 問題（BUG-Q-001）修復 | Felix | Wave 2 release 前 | 視 TC-Q-005 是否必過 | 建議與 SPEC-Q-002 同批 PR（工時小，2.5h） |

---

## 附錄：決策影響矩陣

| 決策 | 影響檔案 | 修改類型 | 時間點 | 負責人 |
|------|---------|---------|--------|--------|
| #1 ChipDTO stockName 移除 | `ChipDTO.java`、`ChipConvertor.java`、`ChipServiceImplTest.java` | 後端 code | Wave 3 前 | Bruno |
| #2 quoteDate 語意補充 | `20260422_schema-lock_stock-detail-apis.md` §1.3 | 文件補充 | Wave 2 release 前 | Jamie / Peter |
| #3 5014 i18n + fatal 分支 | `errorCodes.ts`、`index.tsx`（StockDetail）、`zh-TW.json`、`en.json`、測試檔 | 前端 code | Wave 2 release 前（P0）| Felix |

---

## 變更紀錄

| 版本 | 日期 | 說明 |
|------|------|------|
| v1.0 | 2026-04-23 | Peter 初版，對應 Wave 2 QA Cross-review triage 結果 |

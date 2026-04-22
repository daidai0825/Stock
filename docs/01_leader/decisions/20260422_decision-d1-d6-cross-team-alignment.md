# 決策紀錄：D1-D6 跨團隊對齊仲裁

- **決策日期**：2026-04-22 22:45 (GMT+8)
- **仲裁人**：Jamie（Leader，Stage 6 dual review 後啟動）
- **觸發事件**：Bruno（後端）+ Felix（前端）獨立開發 4 個 endpoints，DTO/errorCode/路徑契約完全不一致
- **影響範圍**：Wave 2（後端）+ Wave B（前端）共 5 個 endpoints
- **後續產出**：Peter SRS schema-lock（`docs/03_spec/20260422_schema-lock_stock-detail-apis.md`）+ errorCodes_central
- **執行結果**：Bruno + Felix Round 2 對齊（已完成，Stage 8 review 進行中）

---

## 衝突盤點（Stage 6 揭露）

| # | 議題 | 後端版本（Bruno） | 前端版本（Felix） | 衝突等級 |
|---|------|-------------------|-------------------|----------|
| 1 | 行情價格欄位 | `closePrice/openPrice/highPrice/lowPrice` | `price/open/high/low/change/changePercent` | 🔴 Blocker |
| 2 | 基本面比率 | `perRatio/pbrRatio` | `per/pbr` | 🔴 Blocker |
| 3 | 法人籌碼結構 | flat: `foreignNetShares/investmentTrustNetShares/dealerNetShares` | `institutions: [{name, buy, sell, netBuySell}]` 陣列 | 🔴 Blocker |
| 4 | History request 形式 | `month: LocalDate` | `period: 'daily'\|'weekly'\|'monthly'` | 🔴 Blocker |
| 5 | 資料來源錯誤碼 | `5010/5011/5012` 段落 | `5101 TWSE_UNAVAILABLE` 單碼 | 🟡 Major |
| 6 | endpoint 路徑 | `/api/v1/quote/history/get` | `/api/v1/quote/history` | 🟡 Minor |

---

## D1：DTO 形式衝突（最大爭議）

### 議題

行情/基本面/籌碼三個 DTO，前後端各有合理性：
- **後端**：closePrice/openPrice 接近原始資料源欄位名（TWSE/MOPS）
- **前端**：price/open/change 符合金融 UI 慣例（TradingView Lightweight Charts 預期欄位）

### 仲裁原則

採 **「最接近終端使用者的層級為準」**：
- 終端使用者 = 散戶投資人（40 歲）
- UI 必須符合金融 App 慣例（避免 closePrice 在 K 線圖上顯示）
- 後端 DTO ≠ 後端 PO，DTO 本來就是為 API consumer 設計

### 決策

🟢 **D1：前端形式採納（Felix-led DTO）**

| Domain | 採納欄位名 |
|--------|-----------|
| Quote | `price`、`previousClose`、`change`、`changePercent`、`open`、`high`、`low`、`volume`、`isStale` |
| Fundamental | `per`、`pbr`、`roe`、`eps`、`reportYear`、`reportQuarter` |
| Chip | `institutions: [{name, buy, sell, netBuySell}]`、`totalNetBuySell` |

### 影響

- Bruno Round 2：14 個 backend 檔案改名 + 重構 ChipDTO
- Felix Round 2：types/stock.ts 加向下相容別名（避免大爆炸）
- PO 層**不動**（DB schema 維持原狀，僅 Convertor 對應）

---

## D2：errorCode 規格衝突

### 議題

前端原本只有 1 個粗粒度 `5101 TWSE_UNAVAILABLE`，但實際資料來源有 4 個：
- TWSE（上市行情）
- MOPS（公開資訊觀測站，基本面）
- CHIP（籌碼來源，可能是 OTC 或 TWSE）
- OTC（櫃買中心）

### 仲裁原則

errorCode 是「給開發者除錯」的訊號，必須能區分故障源頭。後端錯誤碼設計權威性高（後端最清楚故障邊界）。

### 決策

🟢 **D2：後端段落採納（Backend-led errorCode）**

| Code | Domain | Message |
|------|--------|---------|
| 5010 | TWSE | TWSE 服務暫時無法使用 |
| 5011 | MOPS | MOPS 服務暫時無法使用 |
| 5012 | CHIP | 籌碼資料來源異常 |
| 5013 | OTC | OTC 資料來源異常 |
| 5014 | MOPS_FORMAT_CHANGED | MOPS 回傳格式變更（健康檢查告警）|

5101 標 deprecated，前端 errorCodes.ts 移除。

### 影響

- Felix Round 2：errorCodes.ts 全面對齊 + i18n（zh-TW + en.json）補齊
- 後端 ErrorCode.java：補 5013/5014（5010-5012 Wave 2 已加）
- TD-9（errorCode codegen）：Wave 3 改為 Peter SRS → backend ErrorCode + frontend errorCodes.ts 自動產生

---

## D3：History request 形式

### 議題

K 線圖需求：日線/週線/月線切換。
- **後端**：`month: LocalDate` 一次拿一個月日線
- **前端**：`period: 'daily'|'weekly'|'monthly'` 對應 K 線切換 tab

### 仲裁原則

UI flow 主導 → period enum 較直覺；後端為了 weekly/monthly 必須做聚合運算（無法只查 daily 自行加總）。

### 決策

🟢 **D3：period enum 採納，後端做聚合**

```typescript
QuoteHistoryRequest {
  stockId: string
  period: 'daily' | 'weekly' | 'monthly'
  startDate?: string  // ISO 8601
  endDate?: string
}
```

聚合規則：
- daily：直接從 quote_history 查
- weekly：`previousOrSame(MONDAY)` 分組，open=週首日 open、close=週末日 close、high=max、low=min、volume=sum
- monthly：`withDayOfMonth(1)` 分組，規則同上
- 預設範圍：daily 近 3 個月、weekly 近 1 年、monthly 近 5 年

### 影響

- Bruno Round 2：QuoteServiceImpl.getHistory() 完整重寫 + QuoteHistoryAggregationTest（6 測試）
- 已標記 [SRS-CHANGE] 候選：週 K 起算日問題（台股週一假日時）→ 待 Stage 8 review 確認

---

## D4：endpoint 路徑

### 議題

- 後端：`POST /api/v1/quote/history/get`
- 前端：`POST /api/v1/quote/history`

對照 SRS：原文 `/api/v1/quote/history/get`，但 Felix 實作時自動省略 `/get`。

### 仲裁原則

API 路徑是 spec 的一部分，必須跟 SRS 一致。但本案既然發生分歧，藉此檢討 SRS 路徑命名規範。

審視後：`/get` 在 envelope pattern + POST-only 設計下是冗餘（POST `/quote/history` 語意已明確是「取得歷史」）。

### 決策

🟢 **D4：採納前端寫法 `/api/v1/quote/history`，Peter 修正 SRS**

新規範（已寫入 schema-lock 文件）：
- 列表/取得用名詞為主：`/quote/get`、`/quote/list`、`/quote/history`
- 動作型才加動詞後綴：`/order/cancel`、`/user/logout`

### 影響

- Peter：SRS line 557 修正 `/api/v1/quote/history/get` → `/api/v1/quote/history`
- 後端：QuoteController @PostMapping path 改正
- 此規範擴及全部 module（向後檢視時若有冗餘 /get 路徑統一移除）

---

## D5：Schema 權威歸屬（最重要的長期決策）

### 議題

未來如何避免 Bruno + Felix 又各自開發 → DTO 不一致？

### 候選

- A. 前端定 schema → 後端跟隨（D1 邏輯延伸）
- B. 後端定 schema → 前端跟隨（傳統 BFF 思維）
- C. **PM Peter 從 SRS 出發定 schema → 雙方跟隨**（schema-first）
- D. 寫 OpenAPI YAML → 雙方 codegen

### 決策

🟢 **D5：Peter SRS = single source of truth（採 C，未來轉 D）**

**短期執行（Wave 2/B Round 2）**：
- Peter 產出 `docs/03_spec/20260422_schema-lock_stock-detail-apis.md`（DTO 完整定義）
- Peter 產出 `docs/03_spec/20260422_errorCodes_central.md`（37 codes 註冊表）
- 任何 schema 變更必經 [SRS-CHANGE] PR 流程：發 PR 改 SRS → 通知 Bruno + Felix → 雙方對齊 PR

**長期演進（Wave 3+）**：
- TD-9：Peter SRS YAML/JSON → backend ErrorCode + frontend errorCodes.ts 自動產生
- TD（新增）：考慮引入 OpenAPI YAML + openapi-generator

### 影響

- 全團隊工作流程改變：DTO 變更不再單方面
- Bruno + Felix Round 2 + 後續所有 wave 都依此流程
- Peter 的 SRS 角色從「文件員」升級為「契約守門人」

---

## D6：契約測試（防護網）

### 議題

D5 的「人工同步」仍有風險。如何在 CI 階段自動偵測？

### 決策

🟢 **D6：Wave 3 加入 contract test（Pact / Spring Cloud Contract）**

**短期（Round 2）**：
- 暫無自動 contract test
- 改以 Bruno + Felix Round 2 alignment 文件 + Stage 8 dual review 作為人工檢核

**長期（Wave 3 backlog）**：
- TD（新增）：引入 Pact（消費者驅動契約測試）
  - Felix 寫 consumer pact（前端期望的 response shape）
  - Bruno 跑 provider verification（後端確保符合）
  - CI 階段觸發，不一致直接 fail PR

### 影響

- 加入 Wave 3 backlog
- 估時：1-2 工作天（Pact JVM + Pact JS）

---

## 仲裁總結

### 6 個決策的核心精神

1. **D1（前端 DTO）+ D2（後端 errorCode）** — 「最近終端 = 採納者」原則
2. **D3（period enum）** — UI flow 主導，後端負責複雜邏輯
3. **D4（endpoint 路徑）** — 順勢檢討 SRS 規範
4. **D5（Peter 權威）** — schema-first 工作流程
5. **D6（contract test）** — 自動化防護

### 流程改進

| 階段 | Before | After |
|------|--------|-------|
| Spec | Peter 寫 SRS（API 高層次描述） | Peter 寫 SRS + schema-lock（DTO 完整契約） |
| 開發 | Bruno + Felix 各自實作 | 雙方依 schema-lock 實作 |
| 變更 | 任一方臨時調整 | [SRS-CHANGE] PR → Peter 簽核 → 同步公告 |
| 驗證 | dual review 人工核對 | Wave 3 加 contract test 自動驗證 |

### 預防再發生

- **D5**：Peter 在 Spec 階段必輸出 schema-lock
- **D6**：Wave 3 起契約測試入 CI
- 文件樹新增：`docs/03_spec/{date}_schema-lock_*.md` 為標準產出
- Jamie 進度報告增列 schema-lock 產出檢核

---

## 投票紀錄

本次決策由 Jamie 仲裁（非 4 人投票議題，因屬「跨團隊衝突」非「技術選型」）。

依 CLAUDE.md 規則：
> 衝突處理：兩位開發工程師衝突 → 透過 Jamie 協調

故未啟動 Sophia/Preston/Brian/Fiona 投票機制。

但 D5（Peter SRS 權威）涉及流程變動，事後通報 4 位投票成員確認無異議：
- Sophia：✅ 同意（與系統整合契約治理一致）
- Preston：✅ 同意（與專案模組邊界治理一致）
- Brian：✅ 同意（後端 review 受益於明確 spec）
- Fiona：✅ 同意（前端 review 受益於明確 spec）

---

## 後續追蹤

| # | 任務 | 狀態 | 負責人 |
|---|------|------|--------|
| F1 | Peter schema-lock 產出 | ✅ 完成 | Peter |
| F2 | Peter errorCodes_central 產出 | ✅ 完成 | Peter |
| F3 | Bruno Round 2 對齊 | ✅ 完成 | Bruno |
| F4 | Felix Round 2 對齊 | ✅ 完成 | Felix |
| F5 | Stage 8 dual review | 🔄 進行中 | Brian + Fiona |
| F6 | TD-9 errorCode codegen | 📋 Wave 3 | Linus + Peter |
| F7 | contract test (Pact) 引入 | 📋 Wave 3 | Bruno + Felix |
| F8 | API 路徑規範擴及全 module | 📋 待排 | Peter（向後檢視）|

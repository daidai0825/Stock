# Wave 2 Round 2 對齊筆記

**日期**：2026-04-22
**作者**：Bruno（資深後端工程師）
**範圍**：跨團隊 DTO 契約對齊（20260422_schema-lock_stock-detail-apis.md v1.0）

---

## 對齊總覽

| 任務 | 類別 | 狀態 |
|------|------|------|
| A. QuoteDTO 重整 | DTO | 完成 |
| A. QuoteHistoryRequest 重整 | Request | 完成 |
| A. QuoteHistoryResponse 新增 | Response | 完成 |
| A. FundamentalDTO 重整 | DTO | 完成 |
| A. ChipDTO 重構（Flat → Array） | DTO | 完成 |
| B. QuoteServiceImpl period 聚合邏輯 | Service | 完成 |
| B. M-BE-W2-05 market filter 實作 | Service | 完成 |
| C. QuoteConvertor 同步更新 | Convertor | 完成 |
| C. FundamentalConvertor 同步更新 | Convertor | 完成 |
| C. ChipConvertor 重構 | Convertor | 完成 |
| D. ErrorCode.java 確認 | Constant | 確認一致，無需變更 |
| E. 單元測試更新 + 新增 | Test | 完成 |

---

## A. DTO 重整明細

### QuoteDTO（stock-quote 模組）

**修改前**：`openPrice / highPrice / lowPrice / closePrice`（BigDecimal），無 `price / change / changePercent / previousClose / updatedAt / source`。

**修改後**（schema-lock §1.3/§7.1 對齊）：

| 欄位 | 變更說明 |
|------|---------|
| `price` | 新增（原 closePrice → price 重命名，語意：最新成交價） |
| `open` | 重命名（openPrice → open） |
| `high` | 重命名（highPrice → high） |
| `low` | 重命名（lowPrice → low） |
| `previousClose` | 新增（前一交易日收盤價，由 Mapper 查 DB 取得） |
| `change` | 新增（後端計算：price - previousClose，正值帶 "+" 前綴） |
| `changePercent` | 新增（後端計算：change / previousClose * 100，保留 2 位小數） |
| `updatedAt` | 新增（LocalDateTime，對應 PO.createdAt，即 upsert 時間） |
| `source` | 新增（與 market 相同，"TWSE" 或 "OTC"） |
| `isStale` | 維持（已於 Round 1 加入，語義不變） |

### QuoteHistoryRequest

**修改前**：`month: LocalDate`（單月查詢語意）

**修改後**（仲裁 D3）：`period: KLinePeriod` + 可選 `startDate/endDate`

新增 `KLinePeriod` enum（`daily / weekly / monthly`）。

### QuoteHistoryResponse（新增）

新增 `QuoteHistoryResponse` record（schema-lock §3.3）：
- `stockId: String`
- `period: KLinePeriod`
- `items: List<HistoryItem>`

新增 `HistoryItem` record（schema-lock §3.3）：
- `date / open / high / low / close: BigDecimal`
- `volume: Long`

### FundamentalDTO

**修改前**：`perRatio / pbrRatio`（無 `updatedAt / source`）

**修改後**（仲裁 D2 + schema-lock §4.3/§7.2）：

| 欄位 | 變更說明 |
|------|---------|
| `per` | 重命名（perRatio → per） |
| `pbr` | 重命名（pbrRatio → pbr） |
| `updatedAt` | 新增（LocalDateTime，來自 PO.updatedAt） |
| `source` | 新增（固定值 "MOPS"） |

### ChipDTO（InstitutionItem 重構）

**修改前**：Flat 結構（`foreignNetShares / investmentTrustNetShares / dealerNetShares / totalInstitutionalNet / tradeDate`）

**修改後**（仲裁 D2 + schema-lock §5.3/§7.3）：

| 欄位 | 變更說明 |
|------|---------|
| `date` | 重命名（tradeDate → date） |
| `institutions` | 新增（List<InstitutionItem>，固定 3 筆） |
| `totalNetBuySell` | 重命名（totalInstitutionalNet → totalNetBuySell） |
| `source` | 新增（來自 PO.market） |

新增 `InstitutionItem` record：`name / buy / sell / netBuySell`。

> **暫行限制（待 Wave 3）**：PO 未存 buy/sell 明細欄位，InstitutionItem.buy/sell 暫填 0。netBuySell 來自 PO 的 3 個 net 欄位。Wave 3 可補充 PO 欄位與 DB Migration 解決。

---

## B. QuoteServiceImpl 聚合邏輯

### period 聚合規則實作

`getHistory(QuoteHistoryRequest)` 新增三層處理：

1. **參數驗證**：
   - `startDate > endDate` → 拋 1002 PARAM_FORMAT_INVALID
   - 超出最長範圍（daily/weekly ≤ 5 年，monthly ≤ 10 年）→ 拋 1003 PARAM_OUT_OF_RANGE
   - 未帶 startDate 時依 period 套用預設（daily: 90 天，weekly: 1 年，monthly: 5 年）

2. **資料取得**：DB 先查日線，DB miss 才從外部補抓（逐月查詢）

3. **聚合**：
   - `daily`：直接呼叫 `QuoteConvertor.toHistoryItem()` 一對一轉換
   - `weekly`：依 `weekKey()` = `previousOrSame(MONDAY)` 分組，每組取首日 open、最高 high、最低 low、末日 close、加總 volume
   - `monthly`：依 `monthKey()` = `withDayOfMonth(1)` 分組，規則同上

### M-BE-W2-05 market filter 實作

`listQuotes` 回傳前加 `applyMarketFilter(dtoList, market)` 方法，market=null/"ALL" 不過濾，其餘做 case-insensitive 比對。

### previousClose 查詢

新增 `quoteMapper.findPreviousByStockId(stockId, beforeDate)`，查詢指定日期前的最近一筆行情（作為 previousClose）。若查無（首次資料），change/changePercent 設為 0.00。

---

## C. Convertor 更新

### QuoteConvertor

- 主要方法簽名從 `toDTO(po)` 改為 `toDTO(po, previousClose)`（加入 isStale 版本 `toDTOStale(po, previousClose)`）
- `toDTOWithStale()` 私有方法內計算 change/changePercent（保留 2 位小數）
- 新增 `toHistoryItem(po)` 方法（供 daily 聚合使用）

### FundamentalConvertor

- 改為手寫 `default toDTO(po)` 方法（MapStruct 無法自動映射 perRatio→per 等重命名欄位）
- 明確映射：`po.perRatio → dto.per`、`po.pbrRatio → dto.pbr`、`source = "MOPS"` 固定值

### ChipConvertor

- 改為手寫 `default toDTO(po)` 方法
- 將 3 個 flat 欄位轉為 `institutions` 陣列（順序固定：外資、投信、自營商）
- `tradeDate → date`、`totalInstitutionalNet → totalNetBuySell`、`market → source`

---

## D. ErrorCode 對齊確認

比對 `ErrorCode.java` 與 `20260422_errorCodes_central.md`：

| 確認項目 | 結果 |
|---------|------|
| 5013 OTC_DATA_SOURCE_ERROR | 存在（Round 1 已加） |
| 5014 MOPS_FORMAT_CHANGED | 存在（Round 1 已加） |
| 5101 TWSE_UNAVAILABLE | **不存在**（符合 central 廢除記錄） |
| 9003 FEATURE_NOT_AVAILABLE | 存在（Refresh Token stub 用） |
| 3004 FORBIDDEN | 存在（Round 1 已加） |
| 其餘 37 個 code | 逐一比對一致 |

**結論**：ErrorCode.java 與 central 完全一致，本 Round 無需變更。

---

## E. 測試更新

| 測試類別 | 變更說明 |
|---------|---------|
| `QuoteServiceImplTest` | 更新 `buildQuoteDTO` 建構子（新欄位），新增 previousClose 驗證測試 |
| `QuoteHistoryAggregationTest` | **新增**，驗證 daily/weekly/monthly 聚合邏輯 + 參數驗證（1002/1003） |
| `ChipServiceImplTest` | 更新 `buildChipDTO` 建構子（institutions 陣列），新增 `ChipConvertor.toDTO` 直接驗證 |
| `FundamentalServiceImplTest` | 更新 `buildFundamentalDTO` 建構子（per/pbr/updatedAt/source），新增 `FundamentalConvertor.toDTO` 驗證 |

---

## 修改檔案清單

### 修改的檔案

| 模組 | 檔案 | 說明 |
|------|------|------|
| stock-quote | `dto/response/QuoteDTO.java` | 全面重整（schema-lock §1.3） |
| stock-quote | `dto/request/QuoteHistoryRequest.java` | 廢除 month，改為 period + 日期範圍 |
| stock-quote | `convertor/QuoteConvertor.java` | 新增 previousClose 參數、toHistoryItem |
| stock-quote | `service/QuoteService.java` | getHistory 回傳型別改為 QuoteHistoryResponse |
| stock-quote | `service/impl/QuoteServiceImpl.java` | period 聚合、market filter、previousClose 查詢 |
| stock-quote | `controller/QuoteController.java` | getHistory 回傳型別更新 |
| stock-quote | `repository/StockQuoteMapper.java` | 新增 findPreviousByStockId |
| stock-quote | `test/.../QuoteServiceImplTest.java` | 更新建構子、新增 previousClose 驗證 |
| stock-fundamental | `dto/response/FundamentalDTO.java` | per/pbr 去 Ratio、新增 updatedAt/source |
| stock-fundamental | `convertor/FundamentalConvertor.java` | 手寫 toDTO（映射 perRatio→per 等） |
| stock-fundamental | `test/.../FundamentalServiceImplTest.java` | 更新建構子、新增 Convertor 驗證 |
| stock-chip | `dto/response/ChipDTO.java` | Flat → institutions 陣列重構 |
| stock-chip | `convertor/ChipConvertor.java` | 手寫 toDTO（institutions 陣列重構） |
| stock-chip | `test/.../ChipServiceImplTest.java` | 更新建構子、新增 Convertor 驗證 |

### 新增的檔案

| 模組 | 檔案 | 說明 |
|------|------|------|
| stock-quote | `enums/KLinePeriod.java` | daily/weekly/monthly enum |
| stock-quote | `dto/response/HistoryItem.java` | K 線單筆資料（schema-lock §3.3） |
| stock-quote | `dto/response/QuoteHistoryResponse.java` | K 線歷史回應包裝 |
| stock-chip | `dto/response/InstitutionItem.java` | 法人買賣超單筆（schema-lock §5.3） |
| stock-quote | `test/.../QuoteHistoryAggregationTest.java` | weekly/monthly 聚合邏輯新增測試 |

---

## 待 Brian Round 2 Review 重點

| 重點 | 說明 |
|------|------|
| QuoteConvertor.toDTOWithStale | change/changePercent 計算精度（RoundingMode.HALF_UP / 2 位小數） |
| QuoteConvertor.toDTO 簽名變更 | 原本 `toDTO(po)` 改為 `toDTO(po, previousClose)`，所有呼叫點是否都更新 |
| aggregateByKey 的 weekKey/monthKey | 週 key 用 `previousOrSame(MONDAY)` 是否符合台股實際開盤日規則 |
| ChipConvertor.buy/sell 暫填 0 | 確認前端是否能接受 buy=0/sell=0（待 Felix 確認），列為 Wave 3 TD |
| StockQuoteMapper.findPreviousByStockId | 效能：每次 getQuote 多一次 DB 查詢，listQuotes N 個股票 = N 次額外查詢（潛在 N+1） |
| QuoteHistoryResponse cache | cache key 包含 startDate/endDate，若用預設值每次 key 不同（重 cache miss）待確認 |

---

## 潛在風險標記

### [SRS-CHANGE 候選] previousClose 實作說明

Schema-lock §7.1 說明「previousClose 需從 DB 查前一交易日記錄」，但未說明 DB 無前一日時的行為。

**本實作決策**：
- 若 `findPreviousByStockId` 查無（首次資料），previousClose 回傳 `null`。
- Convertor 收到 null 時，prevClose 設為 0，change/changePercent 均為 0.00。
- `QuoteDTO.previousClose` 欄位在此情況下設為 `null`（JSON 序列化為 null）。

**問題**：schema-lock 標記 previousClose 為「必填（Y）」，理論上不應為 null。

若 Peter 確認「首次資料時 previousClose 必填」，則需從 TWSE/OTC 額外補查前一日資料，成本較高。目前以 null 處理，請 Brian 於 Review 時確認是否接受。

### listQuotes N+1 潛在問題

`listQuotes` 中對每個 DB 命中的股票呼叫 `resolvePreviousClose()`，若 N 個股票命中 DB，則有 N 次 `findPreviousByStockId` 查詢。

**Wave 3 TD**：可批次查詢前一交易日所有股票的收盤價（一次 SQL），減少 N 次查詢至 1 次。

# 測試案例：Wave 2 個股詳情頁（M-QUOTE / M-FUND / M-CHIP）

| 項目 | 內容 |
|------|------|
| 撰寫者 | Quincy（資深 QA #1） |
| 撰寫日期 | 2026-04-23 |
| 對應 SRS | `docs/03_spec/20260422_schema-lock_stock-detail-apis.md` v1.0 |
| 對應 errorCode | `docs/03_spec/20260422_errorCodes_central.md` v1.1 |
| 測試環境 | dev |
| 涵蓋模組 | M-QUOTE（行情）/ M-FUND（基本面）/ M-CHIP（籌碼） |
| 前端版本 | Wave B Round 2（commit 1efbc0a）Fiona GO 9.0/10 |
| 後端版本 | Wave 2 Round 2（commit c2ad5bc）+ Bruno hotfix（commit b0d54a0） |

---

## 發現問題與建議（請立刻閱讀）

### BUG-QUINCY-001：ChipDTO 多送 `stockName` 欄位（合約偏差）

- **類型**：Schema Contract Violation
- **嚴重度**：Major（Brian M-02 已識別，列 Wave 3 修正）
- **描述**：`ChipDTO.java` 含 `stockName` 欄位，但 schema-lock §5.3 定義的 5 個欄位中**無此欄位**（`StockChip` TypeScript 型別亦無 `stockName`）。Wave 3 引入 Pact contract test 後將直接 FAIL。
- **建議**：建立 ticket，Wave 3 修正前 QA 執行 TC-CHIP-005 應標記為「預期外行為，待修」。

### BUG-QUINCY-002：ChipDTO `buy` / `sell` 暫填 0（功能缺陷）

- **類型**：Business Logic Gap
- **嚴重度**：Major（schema-lock §5.3 `buy/sell` 為必填欄位）
- **描述**：`ChipConvertor.java` 明確標注 `buy/sell 暫填 0（PO 未存明細，Wave 3 TD）`。schema-lock 要求 `buy: number` / `sell: number` 為有意義的值，目前恆為 0。
- **建議**：Wave 3 補 PO 欄位並移除暫行邏輯；QA 執行 TC-CHIP-002 時，`buy=0/sell=0` 應標注為已知缺陷。

### SPEC-QUINCY-001：`isStale=true` 時前端應顯示哪個 quoteDate？

- **類型**：Spec 模糊
- **描述**：schema-lock §1.3 只說「isStale=true 時顯示『資料延遲，最後更新：{quoteDate}』」，但未規定 `quoteDate` 是「fallback DB 那筆的日期」還是「今日日期」。現行 MSW mock `staleQuote` 用前一個交易日（2026-04-19）作為 quoteDate，後端 fallback 邏輯應確認。
- **建議**：由 Peter 補充 SRS 說明；QA 執行 TC-QUOTE-007 時以「fallback DB 那筆的 quoteDate」為預期值。

---

## 測試範圍總覽

| 模組 | 測試類型 | 案例數 |
|------|----------|--------|
| M-QUOTE getQuote | 功能（手動 + API） | 9 |
| M-QUOTE getHistory | 功能（手動 + API） | 8 |
| M-FUND getFundamental | 功能（手動 + API） | 5 |
| M-CHIP getChip | 功能（手動 + API） | 6 |
| 前端整合 StockDetail | 功能（手動） | 5 |
| 錯誤情境 5010-5014 | 功能（API） | 5 |
| Schema Contract | API 合約驗證 | 4 |
| **合計** | | **42** |

---

## 一、M-QUOTE：`POST /api/v1/quote/get`

### TC-QUOTE-001：TWSE 股票行情查詢成功

- **類型**：功能測試（手動 + API）
- **優先級**：P0
- **對應 AC**：schema-lock §1.3
- **前置條件**：DB 有 stockId=2330 的有效交易日資料；TWSE 外部 API 正常
- **Given**：系統正常運作，DB 含 2330 台積電最新交易日記錄
- **When**：POST `/api/v1/quote/get` 帶 `{ "stockId": "2330" }`，Header 含有效 Bearer token
- **Then**：
  - HTTP status 200
  - `code = 0`
  - `data.stockId = "2330"`
  - `data.stockName = "台積電"`
  - `data.market = "TWSE"`（或 "OTC"，與股票實際掛牌一致）
  - `data.price` 為字串格式（例 `"1050.00"`），非 number
  - `data.change` 正值帶 `+` 前綴（例 `"+19.00"`），負值帶 `-`（例 `"-5.00"`）
  - `data.changePercent` 正值帶 `+` 前綴（例 `"+1.84"`），不含 `%` 符號
  - `data.previousClose` 為字串（例 `"1031.00"`）
  - `data.open / high / low` 皆為字串，不為 number
  - `data.volume` 為整數 number（非字串）
  - `data.quoteDate` 格式 `YYYY-MM-DD`
  - `data.updatedAt` 格式 ISO 8601 含時區（例 `"2026-04-22T13:30:00.000+08:00"`）
  - `data.isStale = false`
  - `data.source` 與 `data.market` 值相同
  - Envelope 含 `timestamp` / `traceId`

### TC-QUOTE-002：OTC 股票行情查詢成功（OTC 路徑驗證）

- **類型**：功能測試（手動 + API）
- **優先級**：P0
- **對應 AC**：schema-lock §1.3 + §1.4
- **前置條件**：DB 有 stockId=6488 廣隆光電（上櫃股）資料
- **Given**：系統正常運作，DB 含 6488 最新交易日記錄
- **When**：POST `/api/v1/quote/get` 帶 `{ "stockId": "6488" }`
- **Then**：
  - `data.market = "OTC"`
  - `data.source = "OTC"`
  - 其餘欄位結構與 TC-QUOTE-001 一致

### TC-QUOTE-003：查無此股票代號（4001）

- **類型**：錯誤情境測試
- **優先級**：P0
- **Given**：DB 無此股票代號；外部 API 亦查無
- **When**：POST `/api/v1/quote/get` 帶 `{ "stockId": "INVALID_ID" }`
- **Then**：
  - HTTP status 200（Envelope 模式）
  - `code = 4001`
  - `message` 含 "查無此股票" 語意
  - 無 `data` 欄位（或 `data = null`）

### TC-QUOTE-004：TWSE 外部 API 不可用（5010 fallback 驗證）

- **類型**：錯誤情境測試
- **優先級**：P0
- **Given**：TWSE 外部 API 模擬不可用（stub 回 5xx 或 timeout）；DB 有最後一筆交易日資料
- **When**：POST `/api/v1/quote/get` 帶有效 TWSE 股票代號
- **Then**（fallback 成功）：
  - `code = 0`
  - `data.isStale = true`
  - `data.quoteDate` 為 DB 最後一筆的交易日（非今日）
- **Then**（DB 也無資料）：
  - `code = 5010`
  - `message` 含 "TWSE" / "暫時無法" 語意

### TC-QUOTE-005：OTC 外部 API 不可用（5013）

- **類型**：錯誤情境測試
- **優先級**：P1
- **Given**：OTC 外部 API 不可用；目標為上櫃股票（6488）
- **When**：POST `/api/v1/quote/get` 帶 `{ "stockId": "6488" }`
- **Then**（fallback 成功）：
  - `code = 0`、`data.isStale = true`
- **Then**（DB 也無資料）：
  - `code = 5013`

### TC-QUOTE-006：缺少必填欄位 stockId（1001）

- **類型**：參數驗證測試
- **優先級**：P1
- **When**：POST `/api/v1/quote/get` 帶 `{}`（空 body）
- **Then**：
  - `code = 1001`
  - `message` 含 "必填" / "required" 語意

### TC-QUOTE-007：isStale=true 時前端顯示警示 Tag

- **類型**：前端整合測試（手動）
- **優先級**：P0
- **前置條件**：後端回傳 `data.isStale = true`
- **Given**：開啟 StockDetail 頁 `/stocks/2330`
- **When**：後端（或 MSW staleQuote mock）回傳 `isStale=true`
- **Then**：
  - 頁面出現帶 WarningOutlined icon 的黃色警示 Tag
  - Tag 文字含 "資料延遲，最後更新：YYYY-MM-DD" 語意
  - `data-testid="stale-data-tag"` 元素可見
  - PriceHeader 仍正常顯示（quoteDate、price 等欄位）

### TC-QUOTE-008：change/changePercent 序列化格式驗證（M-01 hotfix 驗收）

- **類型**：Schema Contract 測試（API）
- **優先級**：P0（M-01 hotfix 核心驗收）
- **When**：呼叫 `/api/v1/quote/get` 取得正漲情境（price > previousClose）
- **Then**：
  - `data.change` 為 JSON string 型別（非 number）
  - `data.change` 值以 `+` 開頭（例 `"+19.00"`）
  - `data.changePercent` 為 JSON string，以 `+` 開頭
- **When**：取得負跌情境（price < previousClose）
- **Then**：
  - `data.change` 以 `-` 開頭（例 `"-5.50"`）
  - 不出現 `"+-"` 雙符號
- **When**：平盤情境（price = previousClose）
- **Then**：
  - `data.change` 為 `"+0.00"`（零值帶 `+`，依 SignedDecimalSerializer 邏輯）

### TC-QUOTE-009：所有 BigDecimal 欄位序列化為 JSON string（非 number）

- **類型**：Schema Contract 測試（API）
- **優先級**：P0
- **When**：呼叫 `/api/v1/quote/get`
- **Then**：以下欄位在 JSON response 中型別為 **string**，不為 number：
  - `price`、`previousClose`、`open`、`high`、`low`、`change`、`changePercent`
- **Then**：以下欄位在 JSON response 中型別為 **number**：
  - `volume`

---

## 二、M-QUOTE：`POST /api/v1/quote/history`

### TC-HISTORY-001：daily 歷史查詢成功

- **類型**：功能測試（手動 + API）
- **優先級**：P0
- **Given**：DB 有 2330 日線記錄
- **When**：POST `/api/v1/quote/history` 帶 `{ "stockId": "2330", "period": "daily", "startDate": "2026-01-01", "endDate": "2026-04-22" }`
- **Then**：
  - `code = 0`
  - `data.stockId = "2330"`
  - `data.period = "daily"`
  - `data.items` 為陣列，每筆含 `date / open / high / low / close / volume`
  - `items` 依 `date` 升冪排序（最舊在前）
  - `items[*].open / high / low / close` 皆為字串（BigDecimal → string）
  - `items[*].volume` 為整數 number

### TC-HISTORY-002：weekly 歷史查詢成功（週聚合驗證）

- **類型**：功能測試（API）
- **優先級**：P0
- **When**：POST `/api/v1/quote/history` 帶 `{ "stockId": "2330", "period": "weekly", "startDate": "2026-04-01", "endDate": "2026-04-22" }`
- **Then**：
  - `data.period = "weekly"`
  - `data.items` 筆數 <= 日線筆數（週聚合後應減少）
  - 每筆 `date` 為該週第一個交易日（不一定是週一，若週一為假日則後移）
  - `volume` 為該週所有交易日成交量加總
  - `high` 為該週最高 high
  - `low` 為該週最低 low

### TC-HISTORY-003：monthly 歷史查詢成功（月聚合驗證）

- **類型**：功能測試（API）
- **優先級**：P0
- **When**：POST `/api/v1/quote/history` 帶 `{ "stockId": "2330", "period": "monthly", "startDate": "2025-01-01", "endDate": "2026-04-22" }`
- **Then**：
  - `data.period = "monthly"`
  - 每筆 `date` 為該月第一個交易日
  - `close` 為該月最後一個交易日的收盤價

### TC-HISTORY-004：period 白名單外的值回 1003

- **類型**：參數驗證測試
- **優先級**：P0（schema-lock §3.4 + MSW R2-03 修正驗收）
- **When**：POST `/api/v1/quote/history` 帶 `{ "stockId": "2330", "period": "yearly" }`
- **Then**：
  - `code = 1003`（PARAM_OUT_OF_RANGE）
  - 不回傳任何資料

### TC-HISTORY-005：startDate > endDate 回 1002

- **類型**：參數驗證測試
- **優先級**：P1
- **When**：POST `/api/v1/quote/history` 帶 `{ "stockId": "2330", "period": "daily", "startDate": "2026-04-22", "endDate": "2026-01-01" }`
- **Then**：`code = 1002`（PARAM_FORMAT_INVALID）

### TC-HISTORY-006：查詢範圍超出 5 年（daily 超限 1003）

- **類型**：邊界測試
- **優先級**：P1
- **When**：POST `/api/v1/quote/history` 帶 `period=daily`，日期範圍 > 1825 天
- **Then**：`code = 1003`

### TC-HISTORY-007：不帶 startDate 使用預設範圍

- **類型**：預設值測試
- **優先級**：P1
- **When**：POST `/api/v1/quote/history` 帶 `{ "stockId": "2330", "period": "daily" }`（無 startDate/endDate）
- **Then**：
  - `code = 0`
  - `data.items[0].date` 距今約 90 天前（daily 預設）

### TC-HISTORY-008：缺少 stockId 或 period（1001）

- **類型**：參數驗證測試
- **優先級**：P1
- **When**：POST `/api/v1/quote/history` 帶 `{ "period": "daily" }`（無 stockId）
- **Then**：`code = 1001`

---

## 三、M-FUND：`POST /api/v1/fundamental/get`

### TC-FUND-001：TWSE 股票基本面查詢成功

- **類型**：功能測試（手動 + API）
- **優先級**：P0
- **Given**：DB 有 2330 近四季 EPS 資料（依年份、季別降序排列）
- **When**：POST `/api/v1/fundamental/get` 帶 `{ "stockId": "2330" }`
- **Then**：
  - `code = 0`
  - `data.stockId = "2330"`
  - `data.stockName` 非空字串
  - `data.eps` 為字串（BigDecimal → string，例 `"43.50"`）
  - `data.per` 為字串，欄位名**不是** `perRatio`（schema-lock §4.3 明確要求）
  - `data.pbr` 為字串，欄位名**不是** `pbrRatio`
  - `data.roe` 為字串（百分比數值，不含 `%`，例 `"23.5"`）
  - `data.reportYear` 為整數 number（例 2025）
  - `data.reportQuarter` 為整數 number，值在 1~4 範圍
  - `data.updatedAt` 格式 ISO 8601 含時區
  - `data.source = "MOPS"`

### TC-FUND-002：OTC 股票基本面查詢（OTC 走 MOPS 路徑）

- **類型**：功能測試（API）
- **優先級**：P0
- **Given**：DB 有 6488 廣隆光電 EPS 資料；上櫃股票仍透過 MOPS 取得基本面
- **When**：POST `/api/v1/fundamental/get` 帶 `{ "stockId": "6488" }`
- **Then**：
  - `code = 0`
  - `data.source = "MOPS"`（OTC 股票基本面來源仍固定為 MOPS）

### TC-FUND-003：EPS 近四季取最新季報（B-BE-W2-03 修正驗收）

- **類型**：業務邏輯測試
- **優先級**：P0（Brian Blocker B-BE-W2-03 修正驗收）
- **Given**：DB 含以下四季資料（刻意亂序存入）：2024Q4 / 2025Q1 / 2024Q2 / 2024Q3
- **When**：POST `/api/v1/fundamental/get` 帶 `{ "stockId": "2330" }`
- **Then**：
  - `data.reportYear = 2025`（最新年份）
  - `data.reportQuarter = 1`（最新季別，不是依排序 get(0)）
  - `data.eps` 為近四季滾動加總（非單季值）

### TC-FUND-004：MOPS 外部 API 不可用（5011）

- **類型**：錯誤情境測試
- **優先級**：P1
- **Given**：MOPS 外部 API 模擬不可用（stub 回 5xx 或 timeout）
- **When**：POST `/api/v1/fundamental/get` 帶有效 stockId
- **Then**：`code = 5011`，message 含 "MOPS" / "暫時無法" 語意

### TC-FUND-005：MOPS 格式變動（5014）

- **類型**：錯誤情境測試
- **優先級**：P1
- **Given**：MOPS 回應格式異動（stub 回 schema 不符的 JSON），後端解析失敗
- **When**：POST `/api/v1/fundamental/get` 帶有效 stockId
- **Then**：
  - `code = 5014`（MOPS_FORMAT_CHANGED）
  - message 含 "格式異動" / "聯絡管理員" 語意
  - 前端不顯示重試按鈕（schema-lock §5xxx 行為：`5014` 需人工介入）

---

## 四、M-CHIP：`POST /api/v1/chip/get`

### TC-CHIP-001：三大法人籌碼查詢成功（TWSE）

- **類型**：功能測試（手動 + API）
- **優先級**：P0
- **Given**：DB 有 2330 最新交易日的三大法人資料
- **When**：POST `/api/v1/chip/get` 帶 `{ "stockId": "2330" }`
- **Then**：
  - `code = 0`
  - `data.stockId = "2330"`
  - `data.date` 格式 `YYYY-MM-DD`（非 datetime）
  - `data.institutions` 為陣列，長度固定 3
  - `data.institutions[0].name = "外資"`
  - `data.institutions[1].name = "投信"`
  - `data.institutions[2].name = "自營商"`（順序嚴格驗證）
  - `data.institutions[*].netBuySell` 為整數 number（正/負/零皆可）
  - `data.totalNetBuySell` 等於三筆 netBuySell 之總和（後端計算）
  - `data.source` 為 `"TWSE"` 或 `"OTC"`

### TC-CHIP-002：buy/sell 欄位驗證（已知缺陷確認）

- **類型**：Schema Contract 測試（API）
- **優先級**：P1
- **說明**：此案例驗證已知缺陷（BUG-QUINCY-002），預期行為為 buy/sell 皆為 0
- **When**：POST `/api/v1/chip/get` 帶有效 stockId
- **Then**：
  - `data.institutions[*].buy` 欄位存在（非 undefined）
  - `data.institutions[*].sell` 欄位存在（非 undefined）
  - **注意**：目前 buy=0 / sell=0 為已知暫行決策（Wave 3 補充）；此案例應標記 "Known Defect BUG-QUINCY-002"

### TC-CHIP-003：totalNetBuySell 計算驗證

- **類型**：業務邏輯測試
- **優先級**：P0
- **When**：POST `/api/v1/chip/get` 帶有效 stockId
- **Then**：
  - 手動計算 `institutions[0].netBuySell + institutions[1].netBuySell + institutions[2].netBuySell`
  - 結果必須等於 `totalNetBuySell`（後端計算，前端不得自行加總）

### TC-CHIP-004：外部籌碼資料不可用（5012），DB 有陳舊資料（isStale fallback）

- **類型**：錯誤情境測試
- **優先級**：P0
- **Given**：籌碼外部 API（TWSE/OTC）不可用；DB 有最後一筆陳舊記錄
- **When**：POST `/api/v1/chip/get` 帶有效 stockId
- **Then**（fallback 成功，對齊 B-BE-W2-02 修正邏輯）：
  - `code = 0`（直接回 DB 最後一筆，不拋錯）
  - 不驗證 isStale（籌碼無此欄位，schema-lock §5.3 未定義）
- **Then**（DB 也無資料）：
  - `code = 5012`

### TC-CHIP-005：ChipDTO 多送 stockName（已知合約偏差確認）

- **類型**：Schema Contract 測試（API）
- **優先級**：Major（記錄用，不阻擋放行）
- **When**：POST `/api/v1/chip/get` 帶有效 stockId
- **Then**：
  - Response body 中**出現** `stockName` 欄位（目前行為）
  - **備註**：schema-lock §5.3 未定義此欄位，屬 BUG-QUINCY-001，Wave 3 修正

### TC-CHIP-006：籌碼來源錯誤（5012，DB 也無資料）

- **類型**：錯誤情境測試
- **優先級**：P1
- **Given**：籌碼外部 API 不可用；DB 也無此股票的籌碼記錄
- **When**：POST `/api/v1/chip/get` 帶有效 stockId
- **Then**：`code = 5012`

---

## 五、前端整合：StockDetail 頁面

### TC-FE-001：頁面正常載入並顯示四個區塊

- **類型**：前端功能測試（手動）
- **優先級**：P0
- **Given**：後端（或 MSW mock）所有 API 回傳成功
- **When**：瀏覽器開啟 `/stocks/2330`
- **Then**：
  - PriceHeader 顯示股票代號、名稱、市場 Tag、價格、漲跌
  - PeriodSelector 顯示三個切換按鈕（日/週/月）
  - KLineChart 渲染 K 線圖（不為空）
  - FundamentalCard 顯示 EPS / PER / PBR / ROE
  - ChipCard 顯示三大法人表格（外資 / 投信 / 自營商）

### TC-FE-002：isStale=true 時顯示警示 Tag

- **類型**：前端功能測試（手動）
- **優先級**：P0
- **Given**：`useStockQuote` 回傳 `data.isStale = true`（使用 MSW staleQuote handler）
- **When**：頁面載入完成
- **Then**：
  - 頁面頂部出現帶 WarningOutlined 的黃色 Tag
  - Tag 文字符合 i18n key `stock.staleData`，含日期資訊
  - `data-testid="stale-data-tag"` 元素可見

### TC-FE-003：4001 股票不存在時顯示空狀態

- **類型**：前端錯誤狀態測試（手動）
- **優先級**：P0
- **Given**：quote/get 回 `code=4001`
- **When**：開啟 `/stocks/INVALID_CODE`
- **Then**：
  - 顯示 `data-testid="stock-not-found-result"` 的 404 Result 元件
  - 含返回按鈕（Link 回首頁）
  - 不顯示任何行情/基本面/籌碼資料

### TC-FE-004：5010/5011/5012 時顯示 notification.error

- **類型**：前端錯誤通知測試（手動）
- **優先級**：P1
- **Given**：任一 API 回傳 5010/5011/5012（使用 MSW errorHandlers）
- **When**：頁面載入
- **Then**：
  - 出現 Ant Design notification.error 元件（右上角）
  - message 含對應錯誤語意（i18n 對應正確）
  - description 含 traceId
  - 5 秒後自動消失

### TC-FE-005：KLine 切換週期

- **類型**：前端互動測試（手動）
- **優先級**：P1
- **When**：在 PeriodSelector 點擊「週」按鈕
- **Then**：
  - 再次呼叫 `/api/v1/quote/history` 帶 `{ "period": "weekly" }`
  - KLineChart 更新為週線資料

---

## 六、錯誤情境完整覆蓋（5010-5014）

| TC ID | errorCode | 觸發端點 | 預期 code |
|-------|-----------|----------|-----------|
| TC-ERR-5010 | TWSE_DATA_SOURCE_ERROR | `/quote/get`（TWSE 股票） | 5010（DB 也無資料時）|
| TC-ERR-5011 | MOPS_DATA_SOURCE_ERROR | `/fundamental/get` | 5011 |
| TC-ERR-5012 | CHIP_DATA_SOURCE_ERROR | `/chip/get`（DB 也無資料） | 5012 |
| TC-ERR-5013 | OTC_DATA_SOURCE_ERROR | `/quote/get`（OTC 股票） | 5013（DB 也無資料時）|
| TC-ERR-5014 | MOPS_FORMAT_CHANGED | `/fundamental/get`（stub 回不合格 JSON）| 5014 |

---

## 七、Schema Contract 驗收（欄位完整性）

### TC-CONTRACT-001：QuoteDTO 欄位完整性

- **When**：`GET /api/v1/quote/get`，code=0
- **Then**：response.data 必須包含且僅包含以下欄位（不多不少）：
  `stockId, stockName, market, price, previousClose, change, changePercent, open, high, low, volume, quoteDate, updatedAt, isStale, source`（共 15 欄位）

### TC-CONTRACT-002：FundamentalDTO 欄位完整性

- **When**：`POST /api/v1/fundamental/get`，code=0
- **Then**：response.data 必須包含：
  `stockId, stockName, eps, per, pbr, roe, reportYear, reportQuarter, updatedAt, source`（共 10 欄位）
- **Then**：response.data 不得含 `perRatio` 或 `pbrRatio` 欄位

### TC-CONTRACT-003：ChipDTO 欄位完整性

- **When**：`POST /api/v1/chip/get`，code=0
- **Then**：response.data 基本欄位：`stockId, date, institutions, totalNetBuySell, source`
- **Then**：`data.institutions` 每一筆必含 `name, buy, sell, netBuySell`
- **注意**：目前多送 `stockName`（BUG-QUINCY-001），驗收時需標記

### TC-CONTRACT-004：HistoryDTO 欄位完整性

- **When**：`POST /api/v1/quote/history`，code=0
- **Then**：response.data 必須包含：`stockId, period, items`
- **Then**：`data.items[*]` 必須包含：`date, open, high, low, close, volume`
- **Then**：`items` 依 date 升冪排序

---

## 附錄：與 Quinn 重疊範圍（整合用）

以下案例屬 E2E 或 UI 互動層面，預期 Quinn 的 Playwright 測試也會涵蓋，整合時去重：

| 本文案例 | 重疊說明 |
|----------|----------|
| TC-FE-001 | Quinn 的 Playwright E2E 必然涵蓋頁面正常載入 |
| TC-FE-002 | isStale Tag 顯示為 UI 層行為，Quinn Playwright 應有 E2E 驗證 |
| TC-FE-003 | 4001 404 頁面，Quinn Playwright E2E 涵蓋 |
| TC-FE-004 | notification.error 顯示，Quinn Playwright 應驗證 toast |
| TC-FE-005 | KLine 週期切換互動，Quinn Playwright E2E 應有端到端驗證 |
| TC-HISTORY-001~003 | K 線歷史資料的 daily/weekly/monthly，如果 Quinn 有後端驗證也重疊 |

**不重疊（Quincy 獨有）**：
- TC-QUOTE-008 / TC-QUOTE-009：BigDecimal 序列化格式，屬 API 層 Postman 測試
- TC-CONTRACT-001~004：Schema contract 欄位完整性，Postman assertion 專屬
- TC-FUND-003：B-BE-W2-03 EPS 排序修正驗收，後端業務邏輯測試
- TC-CHIP-003：totalNetBuySell 計算驗證，API 層測試

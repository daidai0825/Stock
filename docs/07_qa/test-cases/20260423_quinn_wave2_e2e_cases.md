# Quinn Wave 2 E2E 測試案例

| 項目 | 內容 |
|------|------|
| 撰寫者 | Quinn（資深 QA #2） |
| 撰寫日期 | 2026-04-23 |
| 測試範圍 | Wave 2：M-QUOTE / M-FUND / M-CHIP + StockDetail 前端頁面 |
| 對應 Spec | schema-lock v1.0（docs/03_spec/20260422_schema-lock_stock-detail-apis.md） |
| 對應 Spec | errorCodes_central v1.1（docs/03_spec/20260422_errorCodes_central.md） |
| 測試工具 | Playwright E2E |
| Playwright spec 路徑 | tests/e2e/wave2_stockdetail.spec.ts |
| 配置 | playwright.config.ts（Chromium 桌面 / WebKit 桌面 / WebKit iPhone14） |

---

## 潛在問題與 Spec 模糊處（測試前必讀）

> 以下為 Quinn 獨立審閱 schema-lock 與實作程式碼後發現的疑慮，建議建立 ticket 追蹤。

### BUG-Q-001：i18n 語言切換機制未在 spec 明確定義

- 嚴重度：Minor
- 發現位置：前端 i18n 實作 vs. Playwright 測試腳本
- 問題：前端 i18n 透過 `localStorage` 的 `i18nextLng` 切換語言。Playwright 使用 `page.addInitScript` 注入 localStorage，但 `main.tsx` 的 i18n 初始化時機與 MSW worker 啟動之間可能有 race condition（MSW await 完成後才 render，但 i18n 是同步初始化）。
- 建議：在 i18n 模組加入確定性初始化保護，或於 QA 環境提供 `?lang=en` query param 直接覆蓋語言，避免依賴 localStorage 時序。
- 票號建議：WAVE2-QA-001

### BUG-Q-002：MSW errorHandlers 5013/5014 在 Fiona review M-FE-WaveB-R2-02 之前已補齊

- 嚴重度：已解決
- 發現：handlers.ts 第 251-264 行已含 `otcUnavailable` 與 `mopsFormatChanged`（Fiona Minor #2 建議已實作）。
- 確認：確認 handlers.ts 內容，5013 / 5014 errorHandlers 均已存在，Fiona Minor #2 問題已修復。

### BUG-Q-003：MSW period 白名單驗證已補齊（Fiona Minor #3 建議已實作）

- 嚴重度：已解決
- 確認：handlers.ts 第 164-170 行 `allowedPeriods.includes(period)` 白名單檢查已存在，與真實後端行為對齊。

### SPEC-Q-001：Watchlist API 路徑尚未在 schema-lock 中定義

- 嚴重度：Minor
- 問題：TC-Q-007 User Journey 涉及「加入自選股」，但 Watchlist API（`/api/v1/watchlist/add`）未出現在 Wave 2 schema-lock（schema-lock 僅涵蓋 quote/fundamental/chip）。Playwright mock 採推測路徑，Wave 3 實際整合時需更新。
- 建議：Peter 在 Wave 3 spec 中正式定義 Watchlist API schema。

### SPEC-Q-002：5014 MOPS_FORMAT_CHANGED 是否應顯示「重試」按鈕？

- 嚴重度：Minor
- 問題：errorCodes.ts 的 `isDataSourceError` 包含 5010-5014，但 5014 是格式異動（非暫時不可用），需後端介入修復，不應提供重試按鈕。前端 useQueryErrorNotification 目前統一顯示 `common.errorRetry` 文字，未區分 5014。
- 建議：新增 `isFatalDataSourceError(code)` 函式（僅包含 5014），在 useQueryErrorNotification 中判斷顯示不同 description（「請聯絡系統管理員」而非「請稍後重試」）。
- 票號建議：WAVE2-QA-002

---

## 測試案例清單

### TC-Q-001：StockDetail 頁面完整載入

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-001: StockDetail 頁面完整載入')` |
| 優先級 | P0（Blocker 驗收） |
| 作者 | Quinn |
| 對應 schema | schema-lock §1.3 / §3.3 / §4.3 / §5.3 |

#### TC-Q-001-01

```
Given  MSW 預設 handlers 已啟用（VITE_MSW_ENABLED=true）
When   使用者訪問 /stocks/2330
Then   PriceHeader（data-testid="price-header"）可見
And    KLineChart 容器（data-testid="kline-chart-container"）可見
And    FundamentalCard（data-testid="fundamental-card"）可見
And    ChipCard（data-testid="chip-card"）可見
And    price-value 有非空文字內容
And    price-market 文字為 "TWSE" 或 "OTC"
And    kline-disclaimer 含「不構成投資建議」
```

#### TC-Q-001-02

```
Given  訪問 /stocks/2330
When   頁面載入完成
Then   頁面 title / aria-label 包含 "2330"（stockId）
```

---

### TC-Q-002：K 線週期切換

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-002: K 線週期切換')` |
| 優先級 | P0（核心功能） |
| 作者 | Quinn |
| 對應 schema | schema-lock §3.2（period enum）、仲裁 D3 |

#### TC-Q-002-01：預設日線 → 週線切換

```
Given  StockDetail 頁面已載入（/stocks/2330）
And    period-selector 可見
When   使用者點擊「週線」按鈕（data-testid="period-weekly"）
Then   period-weekly 按鈕變為 active（含 ant-radio-button-wrapper-checked class）
And    period-daily 按鈕失去 active 狀態
And    kline-chart-container 持續可見（不閃爍）
```

#### TC-Q-002-02：週線 → 月線切換

```
Given  TC-Q-002-01 完成後（週線 active）
When   使用者點擊「月線」按鈕（data-testid="period-monthly"）
Then   period-monthly 按鈕變為 active
And    period-weekly 按鈕失去 active 狀態
```

#### TC-Q-002-03：月線 → 日線切換（逆向操作）

```
Given  TC-Q-002-02 完成後（月線 active）
When   使用者點擊「日線」按鈕（data-testid="period-daily"）
Then   period-daily 按鈕恢復 active
And    period-monthly 按鈕失去 active 狀態
```

#### TC-Q-002-04：i18n 文字驗證（zh-TW）

```
Given  語言設為 zh-TW（localStorage i18nextLng=zh-TW）
And    StockDetail 頁面已載入
When   確認 period-selector 文字
Then   period-weekly 顯示「週線」
And    period-monthly 顯示「月線」
And    period-daily 顯示「日線」
```

#### TC-Q-002-05：i18n 文字驗證（en）

```
Given  語言設為 en
And    StockDetail 頁面已載入
When   確認 period-selector 文字
Then   period-weekly 顯示「Weekly」
And    period-monthly 顯示「Monthly」
```

---

### TC-Q-003：isStale=true 資料延遲警示

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-003: isStale=true 資料延遲警示')` |
| 優先級 | P0（schema-lock §1.3 設計說明明定） |
| 作者 | Quinn |
| 對應 schema | schema-lock §1.3 isStale 欄位 |

#### TC-Q-003-01：isStale=true 顯示 stale-data-tag

```
Given  quote/get API 回傳 isStale=true、quoteDate="2026-04-19"（前一交易日）
When   使用者訪問 /stocks/2330
And    頁面載入完成
Then   data-testid="stale-data-tag" 元素可見
And    Tag 文字包含 "2026-04-19"
And    Tag 帶有 WarningOutlined icon（.anticon class）
```

#### TC-Q-003-02：isStale=false 不顯示 Tag

```
Given  quote/get API 回傳 isStale=false（預設 MSW handler）
When   使用者訪問 /stocks/2330
And    頁面載入完成
Then   data-testid="stale-data-tag" 不存在於 DOM
```

#### TC-Q-003-03：stale-data-tag 位置在 PriceHeader 之前

```
Given  quote/get API 回傳 isStale=true
When   頁面載入完成
Then   stale-data-tag 的 Y 座標 < price-header 的 Y 座標
```
（驗證 index.tsx §101-113 的 DOM 順序實作正確性）

---

### TC-Q-004：errorCode 5010-5014 Notification（zh-TW）

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-004: errorCode 5010-5014 Notification（zh-TW）')` |
| 優先級 | P0 |
| 作者 | Quinn |
| 對應 i18n | zh-TW.json errors.5010-5014 |

#### TC-Q-004-01：5010 TWSE_DATA_SOURCE_ERROR

```
Given  語言為 zh-TW
And    quote/get API 回傳 { code: 5010, traceId: "mock-5010-001" }
When   使用者訪問 /stocks/2330
Then   notification.error 顯示（.ant-notification-notice-message 可見）
And    通知文字包含「TWSE 資料來源暫時無法存取」
And    通知描述包含 traceId "mock-5010-001"
```

#### TC-Q-004-02：5011 MOPS_DATA_SOURCE_ERROR

```
Given  語言為 zh-TW
And    fundamental/get API 回傳 { code: 5011 }
When   使用者訪問 /stocks/2330
Then   notification 顯示「MOPS 資料來源暫時無法存取」
```

#### TC-Q-004-03：5012 CHIP_DATA_SOURCE_ERROR

```
Given  語言為 zh-TW
And    chip/get API 回傳 { code: 5012 }
When   使用者訪問 /stocks/2330
Then   notification 顯示「籌碼資料來源暫時無法存取」
```

#### TC-Q-004-04：5013 OTC_DATA_SOURCE_ERROR

```
Given  語言為 zh-TW
And    quote/get API 回傳 { code: 5013 }（OTC 股票路徑）
When   使用者訪問 /stocks/6488
Then   notification 顯示「OTC 資料來源暫時無法存取」
```

#### TC-Q-004-05：5014 MOPS_FORMAT_CHANGED

```
Given  語言為 zh-TW
And    fundamental/get API 回傳 { code: 5014 }
When   使用者訪問 /stocks/2330
Then   notification 顯示「MOPS 資料格式異動，請聯絡系統管理員」
```

#### TC-Q-004-06：traceId 透傳驗證

```
Given  quote/get API 回傳 { code: 5010, traceId: "mock-5010-traceid-abc123" }
When   notification 顯示後
Then   notification description 包含 "mock-5010-traceid-abc123"
```
（驗證 BusinessError.traceId 透傳路徑：http.ts → BusinessError → useQueryErrorNotification → notification.error）

---

### TC-Q-005：errorCode 5010-5014 Notification（en）

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-005: errorCode 5010-5014 Notification（en）')` |
| 優先級 | P1 |
| 作者 | Quinn |

#### TC-Q-005-01：5010 英文通知

```
Given  語言為 en
And    quote/get API 回傳 { code: 5010 }
When   使用者訪問 /stocks/2330
Then   notification 顯示「TWSE data source is temporarily unavailable」
```

#### TC-Q-005-02：5014 英文通知

```
Given  語言為 en
And    fundamental/get API 回傳 { code: 5014 }
When   使用者訪問 /stocks/2330
Then   notification 顯示「MOPS data format has changed unexpectedly」
```

---

### TC-Q-006：OTC 股票路徑（6488）

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-006: OTC 股票路徑（6488）')` |
| 優先級 | P1 |
| 作者 | Quinn |
| 對應 schema | schema-lock §1.3 market="OTC"、§4.3 source="MOPS"、§5.3 source="OTC" |

#### TC-Q-006-01：OTC market 標籤

```
Given  quote/get 回傳 market="OTC", source="OTC"（6488 環球晶 mock）
When   使用者訪問 /stocks/6488
And    頁面載入完成
Then   data-testid="price-market" 顯示「OTC」
```

#### TC-Q-006-02：FundamentalCard source=MOPS 顯示

```
Given  fundamental/get 回傳 source="MOPS"（固定值）
When   頁面載入完成
Then   FundamentalCard tooltip icon 的 aria-label 包含「MOPS」
```

#### TC-Q-006-03：基本面數值精確對照

```
Given  fundamental/get 回傳 eps="28.50", per="23.86"
When   頁面載入完成
Then   fundamental-eps 顯示「28.50」
And    fundamental-per 顯示「23.86」
```

#### TC-Q-006-04：法人籌碼三筆固定顯示

```
Given  chip/get 回傳 institutions = [外資, 投信, 自營商]（6488 mock）
When   頁面載入完成
Then   chip-card 包含文字「外資」、「投信」、「自營商」
And    三筆均顯示
```

---

### TC-Q-007：完整 User Journey

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-007: 完整 User Journey')` |
| 優先級 | P1 |
| 作者 | Quinn |

#### TC-Q-007-01：登入後可訪問 StockDetail

```
Given  member/login API mock 回傳 accessToken
When   使用者輸入帳密完成登入
And    前往 /stocks/2330
Then   price-header 可見
And    無跳轉至 /login
```

#### TC-Q-007-02：已登入 PriceHeader 資料完整

```
Given  已登入狀態（token 存在）
And    訪問 /stocks/2330
When   頁面載入完成
Then   price-header 包含「前收盤」或「Prev Close」
And    包含「成交量」或「Volume」
And    包含「更新時間」或「Updated At」
```
（驗證 schema-lock §1.3 所有 UI 必要欄位均顯示）

---

### TC-Q-008：邊界情境

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-008: 邊界情境')` |
| 優先級 | P1-P2 |
| 作者 | Quinn |

#### TC-Q-008-01：空 stockId 跳轉（P1）

```
Given  URL 為 /stocks/（stockId 為空字串）
When   頁面路由解析
Then   不停留在 /stocks/ 路徑
And    跳轉至 /home（或其他非個股頁面）
```
（驗證 index.tsx §72-74：`stockId.length === 0` → Navigate replace）

#### TC-Q-008-02：不存在股票 4001（P0）

```
Given  quote/get API 回傳 { code: 4001, message: "Stock not found" }
When   使用者訪問 /stocks/9999
Then   data-testid="stock-not-found-result" 可見
And    包含「查無此股票代號」或「Stock not found」
```

#### TC-Q-008-03：超長 stockId（100 字元，P2）

```
Given  URL 路徑 stockId 為 100 個字母（邊界值測試）
And    API 回傳 4001
When   頁面載入
Then   不應拋出未處理例外（page crash）
And    頁面正常回應（not-found 或跳轉）
```

#### TC-Q-008-04：所有 API 均中斷（網路斷線，P1）

```
Given  所有 API route（quote/history/fundamental/chip）均 abort
When   使用者訪問 /stocks/2330
Then   12 秒內至少一個 .ant-notification-notice 顯示
And    不出現 page crash
```

#### TC-Q-008-05：period 傳入非白名單值 → API 回 1003（P2）

```
Given  quote/history API mock 接到 period="invalid" 時回傳 { code: 1003 }
When   前端呼叫 history API（模擬惡意請求）
Then   notification 顯示「參數值超出允許範圍」
Note   此情境需透過 Postman / Playwright page.evaluate() 模擬，非正常 UI 操作
```

---

### TC-Q-009：FundamentalCard 欄位完整性

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-009: FundamentalCard 欄位完整性')` |
| 優先級 | P0 |
| 作者 | Quinn |

#### TC-Q-009-01：六欄必填均顯示

```
Given  fundamental/get 回傳完整 StockFundamental（eps/per/pbr/roe/reportYear/reportQuarter/updatedAt）
When   FundamentalCard 渲染完成
Then   fundamental-eps 可見
And    fundamental-per 可見
And    fundamental-pbr 可見
And    fundamental-roe 可見
And    fundamental-report-period 可見
And    fundamental-updated-at 可見
```

#### TC-Q-009-02：PER 標籤文字驗證（禁用 perRatio）

```
Given  語言為 zh-TW
When   FundamentalCard 渲染
Then   Card 文字包含「本益比（PER）」
And    不包含「perRatio」
And    不包含「PERRatio」
```
（驗證 schema-lock §4.3 / §7.2 `per` 欄位名規範）

#### TC-Q-009-03：季報期別格式（YYYY QN）

```
Given  fundamental/get 回傳 reportYear=2025, reportQuarter=4
When   fundamental-report-period 顯示
Then   文字符合格式 /\d{4}\s*Q[1-4]/（例：2025 Q4）
```

#### TC-Q-009-04：EPS 字串格式（BigDecimal）

```
Given  fundamental/get 回傳 eps="43.50"
When   fundamental-eps 顯示
Then   文字包含數字（有小數點格式）
```

---

### TC-Q-010：ChipCard 籌碼完整性

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-010: ChipCard 籌碼完整性')` |
| 優先級 | P0 |
| 作者 | Quinn |

#### TC-Q-010-01：三大法人固定順序

```
Given  chip/get 回傳 institutions = [外資, 投信, 自營商]（schema-lock §5.3）
When   ChipCard table 渲染完成
Then   第 1 筆法人名稱為「外資」
And    第 2 筆法人名稱為「投信」
And    第 3 筆法人名稱為「自營商」
```
（驗證前端不依名稱動態排序）

#### TC-Q-010-02：totalNetBuySell 合計顯示

```
Given  chip/get 回傳 totalNetBuySell=6500000
When   chip-total-net 顯示
Then   元素存在且有數字文字內容
```

#### TC-Q-010-03：買超紅色 / 賣超綠色（台灣慣例）

```
Given  chip/get 回傳含正值（外資 netBuySell=6500000）與負值（自營商 netBuySell=-1700000）
When   ChipCard 渲染
Then   正值（買超）span 含 style color #cf1322（紅）
And    負值（賣超）span 含 style color #3f8600（綠）
```

#### TC-Q-010-04：籌碼日期格式

```
Given  chip/get 回傳 date="2026-04-22"
When   ChipCard 渲染
Then   Card 文字包含符合 YYYY-MM-DD 格式的日期
```

---

### TC-Q-011：行動裝置 RWD

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-011: 行動裝置 RWD')` |
| 優先級 | P1 |
| 作者 | Quinn |
| 測試設備 | iPhone 14 模擬（viewport 390x844） |

#### TC-Q-011-01：手機 viewport 主要區塊可見（含捲動）

```
Given  viewport 為 390x844（iPhone 14）
When   使用者訪問 /stocks/2330
Then   price-header 可見（不需捲動）
And    fundamental-card 可見（捲動後）
And    chip-card 可見（捲動後）
Note   Ant Design Col xs=24 在手機下為 100% 寬，卡片上下排列
```

#### TC-Q-011-02：手機 viewport period-selector 可點擊

```
Given  viewport 為 390x844
When   使用者點擊 period-weekly 按鈕（捲動至可見後）
Then   period-weekly 成為 active 狀態
```

---

### TC-Q-012：PriceHeader 漲跌顏色（台灣股市慣例）

| 欄位 | 內容 |
|------|------|
| Playwright spec | `describe('TC-Q-012: PriceHeader 漲跌顏色')` |
| 優先級 | P1 |
| 作者 | Quinn |

#### TC-Q-012-01：漲（change 正值）→ 紅色 #cf1322

```
Given  quote/get 回傳 change="+105.00"（正值漲幅）
When   price-value 渲染
Then   price-value 的 style 含 color: #cf1322（台灣紅漲）
```

#### TC-Q-012-02：跌（change 負值）→ 綠色 #3f8600

```
Given  quote/get 回傳 change="-105.00"（負值跌幅）
When   price-value 渲染
Then   price-value 的 style 含 color: #3f8600（台灣綠跌）
```

#### TC-Q-012-03：平盤（+0.00）→ 無顏色

```
Given  quote/get 回傳 change="+0.00"
When   price-value 渲染
Then   price-value 的 style 不含 #cf1322
And    不含 #3f8600
Note   驗證 getPriceDirection() 函式中 "+0.00" 被判定為 "flat"
```

---

## 測試矩陣摘要

| TC 編號 | 描述 | 優先級 | 瀏覽器 | Playwright spec describe |
|---------|------|--------|--------|--------------------------|
| TC-Q-001 | 頁面完整載入 | P0 | 全部 | TC-Q-001 |
| TC-Q-002 | K 線週期切換 | P0 | 全部 | TC-Q-002 |
| TC-Q-003 | isStale 警示 Tag | P0 | 全部 | TC-Q-003 |
| TC-Q-004 | 5010-5014 Notification（zh-TW）| P0 | 全部 | TC-Q-004 |
| TC-Q-005 | 5010-5014 Notification（en）| P1 | Chromium | TC-Q-005 |
| TC-Q-006 | OTC 股票（6488）| P1 | 全部 | TC-Q-006 |
| TC-Q-007 | User Journey 登入→詳情 | P1 | Chromium | TC-Q-007 |
| TC-Q-008 | 邊界情境 | P1-P2 | Chromium | TC-Q-008 |
| TC-Q-009 | FundamentalCard 欄位 | P0 | 全部 | TC-Q-009 |
| TC-Q-010 | ChipCard 完整性 | P0 | 全部 | TC-Q-010 |
| TC-Q-011 | 行動裝置 RWD | P1 | WebKit Mobile | TC-Q-011 |
| TC-Q-012 | 漲跌顏色台灣慣例 | P1 | Chromium | TC-Q-012 |

**合計：12 大 TC / 37 小 case（含 35 Playwright it() 場景）**

---

## 與 Quincy 重疊範圍（整合用）

以下案例與 Quincy 分工中功能測試部分可能重疊，整合時需去重或保留更詳細版本：

| Quinn TC | 重疊可能項目 | 說明 |
|----------|-------------|------|
| TC-Q-001（頁面載入） | Quincy 功能測試基礎案例 | 相同 Happy Path，Quinn 版本為 E2E（Playwright）；Quincy 版本為單元/整合測試。去重時保留兩者（層次不同） |
| TC-Q-004（5010-5014 zh-TW notification）| Quincy Postman API 錯誤碼驗證 | Quincy 驗證後端回傳 code 正確；Quinn 驗證前端 notification 文字顯示。不重疊（層次不同，均保留） |
| TC-Q-009（FundamentalCard 欄位）| Quincy 功能測試可能含 per/pbr 欄位名驗證 | 若 Quincy 有相同案例，合併後以 Quinn E2E 版本（含 DOM 驗證）為主 |
| TC-Q-010（ChipCard 順序）| Quincy 可能含三大法人順序驗證 | 確認 Quincy 是否有相同 case，若有則整合時標記「Quinn E2E / Quincy 功能測試雙重覆蓋」 |
| TC-Q-008-02（4001 STOCK_NOT_FOUND）| Quincy API 測試含 4001 scenario | Quincy 驗證後端；Quinn 驗證前端 UI 呈現（stock-not-found-result），不重疊 |

---

## 執行指令

```bash
# 安裝 Playwright
cd /usr/local/dale/daidai0825/Stock
npm install -D @playwright/test
npx playwright install chromium webkit

# 啟動前端 dev server（含 MSW）
cd frontend && VITE_MSW_ENABLED=true npm run dev &

# 執行全部 E2E 測試
cd /usr/local/dale/daidai0825/Stock
npx playwright test tests/e2e/wave2_stockdetail.spec.ts

# 執行指定 TC
npx playwright test tests/e2e/wave2_stockdetail.spec.ts -g "TC-Q-003"

# 指定瀏覽器
npx playwright test --project=chromium-desktop
npx playwright test --project=webkit-mobile-iphone14

# 產出 HTML 報告
npx playwright test --reporter=html
npx playwright show-report playwright-report
```

---

## 尚待確認事項

1. Playwright 是否加入 `devDependencies`（package.json 目前無 `@playwright/test`，需由 Linus 評估）
2. CI（Jenkins）是否在 dev 環境跑 E2E（目前 Jenkinsfile 前端段有 E2E stage，但需確認 MSW 啟動方式）
3. 語言切換機制（SPEC-Q-001）：需 Felix 確認 i18n 初始化時序，或提供 `?lang=` query param 支援
4. Watchlist API schema（SPEC-Q-001）：Wave 3 spec 正式定義後，TC-Q-007 mock 路徑需更新

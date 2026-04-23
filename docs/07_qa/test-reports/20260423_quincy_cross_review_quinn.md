# Cross-Review 報告：Quincy 審查 Quinn 的 Wave 2 E2E 測試成果

| 項目 | 內容 |
|------|------|
| 審查者 | Quincy（資深 QA #1） |
| 被審查者 | Quinn（資深 QA #2） |
| 審查日期 | 2026-04-23 |
| 被審查成果 | `docs/07_qa/test-cases/20260423_quinn_wave2_e2e_cases.md`（12 TC / 37 case）<br>`tests/e2e/wave2_stockdetail.spec.ts`（1119 行） |
| 對照文件 | `docs/07_qa/test-cases/20260423_quincy_wave2_test-cases.md`（42 案例） |
| 測試基準 | schema-lock v1.0、errorCodes_central v1.1 |

---

## 1. 重疊矩陣

> 欄位說明：「層次」指兩個測試案例是否在相同抽象層驗證。「是否重複」以實際驗證標的判斷，同一行為在 API 層與 UI 層驗證屬不同層，不算重複。

| Quinn TC（E2E） | 對應 Quincy TC | 重疊類型 | 說明 | 整合建議 |
|----------------|----------------|----------|------|----------|
| TC-Q-001-01（頁面四區塊可見） | TC-FE-001 | UI 層完全重疊 | 都在驗證頁面正常載入後四個卡片存在；但 Quinn 用 Playwright DOM 驗證，Quincy 用手動測試描述。功能等效。 | 合併：以 Quinn E2E 版本為主責，Quincy TC-FE-001 降級為手動補充，消除重複自動化。 |
| TC-Q-001-02（頁面 title 含 2330） | - | Quinn 獨有 | Quincy 無此案例 | 保留 Quinn，補入整合清單 |
| TC-Q-002（K 線週期切換全程） | TC-FE-005 | UI 層完全重疊 | Quincy TC-FE-005 描述「點擊週按鈕後再呼叫 history API」，Quinn 驗證三向切換 + 按鈕 active 狀態。Quinn 覆蓋更廣。 | Quinn E2E 為主；Quincy TC-FE-005 標記「已由 Quinn E2E 覆蓋，僅保留 API request 驗證部分」 |
| TC-Q-003-01（isStale=true Tag 顯示） | TC-FE-002 / TC-QUOTE-007 | UI 層重疊 + API 層不同 | Quincy TC-QUOTE-007 驗證後端 API 回傳 isStale=true；TC-FE-002 驗證前端 Tag 顯示（手動）。Quinn TC-Q-003 透過 page.route() 精確 mock 後驗證 DOM，層次最完整。 | 三案例保留，但整合報告中標記各案例驗證層次；Quinn 為 E2E 主責，Quincy TC-QUOTE-007 為後端主責 |
| TC-Q-003-03（stale-data-tag Y 座標） | - | Quinn 獨有 | Quincy 無 DOM 位置驗證；此案例驗證 index.tsx 第 101-113 行 DOM 順序 | 保留 Quinn |
| TC-Q-004-01~05（5010-5014 zh-TW notification） | TC-FE-004 / TC-ERR-5010~5014 | 不同層，均保留 | Quincy TC-ERR 驗證後端 API 回傳正確 code；TC-FE-004 驗證前端通知顯示（手動描述）。Quinn TC-Q-004 用 Playwright 驗證 i18n 文字精確值，覆蓋深度最高。 | 三層均保留，Quincy TC-FE-004 可精簡為「參見 Quinn TC-Q-004」，後端驗證仍由 Quincy Postman 負責 |
| TC-Q-004-06（traceId 透傳 notification） | - | Quinn 獨有 | Quincy 無此 Playwright 層驗證（Postman 只驗回傳，不驗 UI description 中的 traceId） | 保留 Quinn，列入整合清單 |
| TC-Q-005（5010/5014 en notification） | - | Quinn 獨有 | Quincy 無英文 i18n 驗證案例 | 保留 Quinn，Quincy 應補充 |
| TC-Q-006-01（OTC market 標籤） | TC-QUOTE-002 | API + UI 不同層 | Quincy 驗證 API 回傳 market="OTC"；Quinn 驗證 UI data-testid="price-market" 文字為 "OTC" | 兩案例保留 |
| TC-Q-006-03（EPS 28.50/PER 23.86 精確值） | TC-FUND-001（部分） | 相似但層次不同 | Quincy 驗證 API 欄位；Quinn 驗證 UI 顯示數值 | 兩案例保留 |
| TC-Q-007-01（登入後存取 StockDetail） | - | Quinn 獨有 | Quincy 未有完整 User Journey E2E | 保留 Quinn |
| TC-Q-008-01（空 stockId 跳轉） | - | Quinn 獨有 | Quincy 未涵蓋路由層邊界 | 保留 Quinn |
| TC-Q-008-02（4001 UI not-found-result） | TC-FE-003 / TC-QUOTE-003 | UI 層重疊 + API 層不同 | Quincy TC-QUOTE-003 驗證後端回 4001；TC-FE-003 驗證前端顯示 404 元件（手動）。Quinn 用 Playwright 精確驗證 | Quinn E2E 為主責 UI 層；Quincy 繼續負責後端 API 層 |
| TC-Q-008-04（所有 API abort 網路斷線） | - | Quinn 獨有 | Quincy 無全斷網情境 | 保留 Quinn |
| TC-Q-009-01（六欄顯示） | TC-FE-001（部分）/ TC-FUND-001 | 部分重疊 | Quincy TC-FUND-001 驗證 API 欄位結構；TC-FE-001 手動描述 EPS/PER 顯示。Quinn 用 Playwright 逐 testid 驗證六個欄位 | Quinn E2E 為主責；Quincy TC-FE-001 中基本面部分可簡化為參照 TC-Q-009 |
| TC-Q-009-02（禁用 perRatio/PERRatio） | TC-CONTRACT-002（部分）/ TC-FUND-001 | 相似但層次不同 | Quincy TC-CONTRACT-002 驗證 API response.data 不得含 `perRatio` 欄位；Quinn 驗證 UI 文字不出現此字串 | 兩案例保留，層次互補 |
| TC-Q-010-01（三大法人固定順序） | TC-CHIP-001 | API + UI 不同層 | Quincy 驗證 API institutions[0,1,2].name 順序；Quinn 驗證 UI table 中順序 | 兩案例保留 |
| TC-Q-010-03（買超紅/賣超綠） | - | Quinn 獨有 | Quincy 無顏色樣式驗證案例 | 保留 Quinn |
| TC-Q-012（漲跌平盤三色驗證） | - | Quinn 獨有 | Quincy 無 CSS color 驗證案例 | 保留 Quinn，列入整合清單 |
| TC-Q-011（行動裝置 RWD） | - | Quinn 獨有 | Quincy 無行動裝置測試 | 保留 Quinn |

**重疊統計：**
- 完全重疊（同層、同行為）：2 項（TC-Q-001-01、TC-Q-002 vs Quincy TC-FE-001 / TC-FE-005）
- 部分重疊（不同層互補）：9 項
- Quinn 獨有：12 項
- Quincy 獨有（詳見第 4 節）：14 項

---

## 2. Quinn 漏測項目（API/Contract 視角）

以下為從我的 Postman / API contract 視角看，Quinn 的 E2E 測試未覆蓋或覆蓋不足的部分。

### 2-1. BigDecimal 序列化格式（Critical，P0）

**Quincy TC-QUOTE-008 / TC-QUOTE-009 未被 Quinn 覆蓋。**

Quinn 的 E2E 僅驗證 UI 文字包含數字，未驗證 API response 的 JSON 型別（string vs number）。

- `TC-Q-001`、`TC-Q-009` 只做 `toContain('28.50')` 文字比對
- 實際 contract 要求：`price / change / changePercent / eps / per / pbr / roe` 必須在 JSON 中為 **string 型別**，`volume / reportYear / reportQuarter` 為 **number 型別**
- 此差異是 Wave 2 Round 1 M-01 hotfix 的核心驗收項目（change="+19.00" 序列化）
- Quinn 的 Playwright 無法驗證 HTTP response body 的 JSON 型別

**建議：** Wave 3 引入 Pact contract test 之前，此項由 Quincy Postman TC-QUOTE-008/009 獨立負責，整合報告中明確標注「E2E 無法涵蓋，由 API 層主責」。

### 2-2. 週/月聚合邏輯正確性（P0，Quincy TC-HISTORY-002/003）

- Quinn TC-Q-002 只驗證「期別切換後按鈕 active 狀態 + 圖表不閃爍」
- 未驗證後端週/月聚合是否正確：weekly items 筆數應 <= daily 筆數、每週 volume 為加總、monthly close 為月末收盤價
- 此為後端業務邏輯驗證，E2E 工具受限（UI 只顯示圖表，無法直接讀取 K 線資料點）

**建議：** 由 Quincy Postman 主責；Quinn 可考慮補充 intercepting history API response 驗證 items 筆數合理性（弱驗證）。

### 2-3. FundamentalDTO EPS 近四季排序邏輯（P0，TC-FUND-003）

- Quinn 僅驗證 fundamental-eps 顯示值、格式
- 未驗證 B-BE-W2-03 修正：「DB 亂序存入時，取最新季報（非 get(0)）」
- 此為後端業務邏輯，E2E mock 固定回傳值無法驗證排序選取行為

**建議：** 此項為 Quincy Postman 專屬，在整合清單中明確標注。

### 2-4. totalNetBuySell 算術正確性（P0，TC-CHIP-003）

- Quinn TC-Q-010-02 驗證 `chip-total-net` 有數字文字
- 未驗證 `totalNetBuySell === institutions[0].netBuySell + institutions[1].netBuySell + institutions[2].netBuySell`
- E2E 中 Quinn 的 6488 mock 提供 totalNetBuySell=1_500_000 但三筆個別為 1_500_000+300_000+(-300_000)=1_500_000，總和剛好一致。但 2330 預設 mock 未提供精確 totalNetBuySell 數值驗證

**建議：** 在 TC-Q-010 補充算術驗證（讀取三筆 netBuySell → 加總 → 比對 chip-total-net 顯示值）。或由 Quincy Postman TC-CHIP-003 主責。

### 2-5. History API 預設範圍行為（P1，TC-HISTORY-007）

- Quinn 未測試不帶 startDate/endDate 時使用預設 90 天範圍
- 此為後端 QuoteHistoryRequest 預設值邏輯，E2E 很難驗證，但 API 層必須驗收

**建議：** Quincy Postman 主責，整合清單標注。

### 2-6. isStale 對應的 quoteDate 語意（SPEC-QUINCY-001）

- Quinn TC-Q-003-01 驗證 Tag 文字包含 "2026-04-19"（與 mock 值吻合）
- 但未驗證此日期來自「fallback DB 那筆的 quoteDate」而非「今日日期」
- 後端 fallback 邏輯的 quoteDate 語意需在 Postman TC-QUOTE-004 中驗收

**建議：** Quincy TC-QUOTE-004 補充：fallback 時 `data.quoteDate` 必須為 DB 最後一筆的日期，非 `LocalDate.now()`。

### 2-7. 5014 「不顯示重試按鈕」行為（P1，TC-FUND-005 的前端部分）

- Quincy TC-FUND-005 要求「前端不顯示重試按鈕（5014 需人工介入）」
- Quinn SPEC-Q-002 發現此問題並建議改進，但 TC-Q-004-05 / TC-Q-005-02 **未加入「不含重試按鈕」的斷言**
- TC-Q-005-02 的 test description 雖提及「不含重試提示」，但 spec.ts 第 504 行只做 `expectNotificationContains(...)`，未驗證 description 文字不包含重試相關字串

**這是 Quinn E2E 的斷言缺漏（後詳見第 5 節品質評分）。**

### 2-8. ChipDTO 多送 stockName 合約偏差（BUG-QUINCY-001）

- Quinn 完全未觸及此已知偏差的 E2E 驗證
- TC-CHIP-005 是 Quincy 獨有的合約偏差記錄案例
- E2E 層面難以主動驗證「不應出現的欄位」，此項確實由 Quincy Postman 專屬

---

## 3. Quincy 應吸收 Quinn 的優點

### 3-1. 漲跌顏色驗證（TC-Q-012）

Quincy 在測試案例中完全缺乏 CSS 樣式驗證。Quinn TC-Q-012 用 `getAttribute('style')` 驗證 `price-value` 的顏色值（#cf1322 / #3f8600），以及平盤無顏色的三種情境，這是純 E2E 工具才能做的驗證。

**Quincy 應新增**：在整合案例中新增一個手動測試案例記錄此行為，並標記「由 Quinn E2E TC-Q-012 自動化」。

### 3-2. DOM 位置驗證技法（TC-Q-003-03）

Quinn 用 `boundingBox()` 比較 Y 座標驗證元素 DOM 順序，這是 Quincy 在手動測試描述中不會觸及的。後續如有 UI 佈局 AC（例如 schema-lock 說明 stale-data-tag 要在 PriceHeader 之前），應善用此技法。

### 3-3. page.route() 精確覆蓋各 error code（TC-Q-004 / TC-Q-012）

Quinn 對每個 test 使用 `page.route()` 獨立 mock API 回應，每個 it() 完全自包含，測試隔離性好。Quincy 的功能測試描述中「前置條件：使用 MSW errorHandlers」寫法較鬆散，在自動化腳本撰寫時應採 Quinn 的精確 mock 做法。

### 3-4. i18n 雙語系驗證（TC-Q-002-04/05、TC-Q-004/005）

Quinn 系統性地對每個功能做 zh-TW + en 兩語驗證，且使用 `beforeEach` 集中設定語言。Quincy 目前無英文 i18n 驗證案例。

**Quincy 應新增**：TC-FE-004 補充 en 語系的 notification 文字驗證（或直接標記「由 Quinn TC-Q-005 主責」）。

### 3-5. 行動裝置 RWD（TC-Q-011）

Quincy 完全未涵蓋行動裝置測試。Quinn 在 playwright.config.ts 設定 iPhone 14 project（viewport 390x844），並加入 `scrollIntoViewIfNeeded()` 確保卡片捲動後可見，這是 Quincy 應學習的模式。

### 3-6. BUG 與 SPEC 提案的品質

Quinn 發現的 BUG-Q-001（i18n race condition）和 SPEC-Q-002（5014 不應顯示重試按鈕）是從實作細節出發的觀察，品質高。SPEC-Q-002 的 `isFatalDataSourceError(code)` 建議具體可行。Quincy 在撰寫 Bug 報告時應採相同格式：問題位置 → 復現步驟 → 具體建議 → 票號。

---

## 4. Quincy 獨有（Quinn 未覆蓋）

以下 14 項案例是 Quincy 的 Postman / API 層測試，Quinn 的 E2E 無法替代：

| Quincy TC | 說明 | 優先級 |
|-----------|------|--------|
| TC-QUOTE-008 | change/changePercent 為 JSON string 型別，含 +/- 前綴 | P0 |
| TC-QUOTE-009 | 所有 BigDecimal 欄位為 string，volume 為 number | P0 |
| TC-HISTORY-001~003 | daily/weekly/monthly 聚合邏輯：筆數、volume 加總、close 語意 | P0 |
| TC-HISTORY-004 | period 非白名單值回 1003 | P0 |
| TC-HISTORY-005 | startDate > endDate 回 1002 | P1 |
| TC-HISTORY-006 | 查詢範圍超出 5 年回 1003 | P1 |
| TC-HISTORY-007 | 不帶 startDate 使用預設 90 天範圍 | P1 |
| TC-FUND-003 | EPS 近四季取最新季報，B-BE-W2-03 修正驗收 | P0 |
| TC-CHIP-003 | totalNetBuySell = 三法人 netBuySell 算術加總 | P0 |
| TC-CHIP-005 | ChipDTO 多送 stockName 合約偏差記錄（BUG-QUINCY-001） | Major |
| TC-CONTRACT-001 | QuoteDTO 15 欄位完整性（不多不少） | P0 |
| TC-CONTRACT-002 | FundamentalDTO 欄位完整性，不含 perRatio/pbrRatio | P0 |
| TC-CONTRACT-003 | ChipDTO 欄位完整性（含 buy/sell/netBuySell） | P0 |
| TC-CONTRACT-004 | HistoryDTO 欄位完整性，items 升冪排序 | P0 |

---

## 5. Quinn E2E 品質評分

### 5-1. 測試命名（10/10）

所有 `test.describe` 標題格式一致（`TC-Q-NNN: 功能描述`），`test()` 描述為「情境 → 預期」句式，可讀性高。輔助函式名稱（`waitForStockDetailLoaded`、`expectNotificationContains`、`setLanguage`）語意清楚。

### 5-2. Selector 穩定性（9/10）

絕大多數使用 `page.getByTestId()`（data-testid 選取），不依賴易變的 CSS class 或文字內容。例外：

- TC-Q-010-01 使用 `chipCard.locator('td').filter({ hasText: /外資|投信|自營商/ })` 依賴 table 結構，若元件改用 `<ul>` 或 `<div>` 則 selector 失效。建議補充對應的 `data-testid="chip-institution-row-{name}"` 需求。
- TC-Q-002 使用 `toHaveClass(/ant-radio-button-wrapper-checked/)` 依賴 Ant Design 內部 class 名稱，版本升級後可能破壞。建議改用 `aria-checked="true"` 或補充 `data-testid="period-weekly-checked"` 屬性驗證。

### 5-3. 斷言完整性（7/10）

優點：

- TC-Q-003-03（stale-data-tag DOM 位置用 boundingBox 比較 Y 座標）
- TC-Q-012（三種漲跌情境分開驗證，包含平盤無顏色）
- TC-Q-004-06（traceId 透傳到 notification description）

缺漏：

- **TC-Q-005-02 描述稱「不含重試提示」但斷言只有 `expectNotificationContains`，未加入 `expect(descText).not.toContain('重試')` 或類似斷言**（SPEC-Q-002 提案的驗收未落地）
- TC-Q-008-03（超長 stockId）斷言為 `expect(true).toBe(true)`，等同無斷言，只驗證「不 crash」，缺乏實質驗收標準
- TC-Q-010-02（totalNetBuySell 合計）只驗證 `text.match(/\d/)` 有數字，未驗證數值是否等於三法人加總
- TC-Q-007-01（登入後存取 StockDetail）用 `catch(() => {})` 吞掉登入跳轉失敗，導致測試永遠通過，無論登入是否成功

### 5-4. 跨瀏覽器覆蓋（9/10）

playwright.config.ts 設定三個 project（Chromium 桌面 / WebKit 桌面 / WebKit iPhone14），與 schema-lock 的跨瀏覽器 AC 對齊。

TC-Q-005（en notification）和 TC-Q-007（User Journey）標記為 Chromium only，合理（i18n 行為跨瀏覽器差異極小，降低重複執行成本）。TC-Q-011 強制 mobile viewport（`test.use({ viewport })`），比在 config 設定 project 更精確。

唯一扣分：Firefox（Gecko）完全未涵蓋，spec 文件中三個 browser 列表無 Firefox，是顯著覆蓋缺口。波段 2 需求中如有 Firefox 使用者（B2C 產品通常需要），應補充。

### 5-5. 測試隔離性與維護性（9/10）

- 每個 test 用 `page.route()` 自包含 mock，不依賴前一 test 狀態
- 輔助函式提取到模組頂部，可重用性高
- TC-Q-006 用 `test.beforeEach` 設定 4 個 API mock，結構清晰
- 常數提取（`STOCK_2330_URL`、`QUOTE_API` 等）避免硬碼重複

微小缺點：TC-Q-003-01/02/03 分別重複定義幾乎相同的 `page.route(QUOTE_API, ...)` mock body，可提取為 shared fixture function。

### 5-6. Bug 發現品質（9/10）

BUG-Q-001（i18n race condition）識別了 MSW worker 與 i18n 初始化的時序問題，是 Quincy 測試描述中沒有觸及的維度。SPEC-Q-002（5014 不應顯示重試按鈕）有具體實作建議（`isFatalDataSourceError`）。SPEC-Q-001（Watchlist API 路徑未在 schema-lock 定義）是跨 Wave 的依賴識別，有助於 Wave 3 規劃。

---

## 6. 總分

| 評分維度 | 滿分 | 得分 |
|----------|------|------|
| 測試命名 | 10 | 10 |
| Selector 穩定性 | 10 | 9 |
| 斷言完整性 | 10 | 7 |
| 跨瀏覽器覆蓋 | 10 | 9 |
| 測試隔離性與維護性 | 10 | 9 |
| Bug 發現品質 | 10 | 9 |
| **合計** | **60** | **53** |
| **換算 10 分制** | | **8.8 / 10** |

---

## 7. 整合後去重建議（哪些 case 由誰主責）

### UI / E2E 層（Quinn 主責）

| 整合後 TC | 主責 | 補充說明 |
|-----------|------|----------|
| 頁面完整載入（四區塊） | Quinn TC-Q-001 | Quincy TC-FE-001 保留為手動補充 |
| K 線週期切換（三向 + active 狀態） | Quinn TC-Q-002 | Quincy TC-FE-005 保留 API request 驗證部分 |
| isStale Tag 顯示（含 Y 座標位置） | Quinn TC-Q-003 | Quincy TC-QUOTE-007 負責後端回傳語意 |
| 5010-5014 notification zh-TW + en | Quinn TC-Q-004/005 | Quincy TC-FE-004 降級為手動補充 |
| OTC market 標籤 UI 顯示 | Quinn TC-Q-006-01 | Quincy TC-QUOTE-002 負責後端 API 層 |
| 基本面欄位六欄顯示 | Quinn TC-Q-009-01 | Quincy TC-FUND-001 負責 API 欄位型別 |
| 禁用 perRatio/PERRatio 標籤 | 雙重覆蓋（Quinn UI 文字 + Quincy API 欄位名稱） | 均保留，層次互補 |
| 三大法人固定順序（UI table） | Quinn TC-Q-010-01 | Quincy TC-CHIP-001 負責 API 層順序 |
| 買超紅 / 賣超綠 / 平盤無色 | Quinn TC-Q-010-03 / TC-Q-012 | Quincy 無對應案例，Quinn 獨立主責 |
| RWD 行動裝置 | Quinn TC-Q-011 | Quincy 無對應案例 |
| User Journey 登入 + 頁面存取 | Quinn TC-Q-007 | Quincy 無對應案例 |
| 4001 not-found UI | Quinn TC-Q-008-02 | Quincy TC-FE-003 降級為手動補充；TC-QUOTE-003 負責後端 |
| 空 stockId 跳轉 + 超長 stockId | Quinn TC-Q-008-01/03 | Quincy 無對應案例 |
| 網路完全中斷 | Quinn TC-Q-008-04 | Quincy 無對應案例 |

### API 層（Quincy 主責）

| Quincy TC | 說明 |
|-----------|------|
| TC-QUOTE-001~006 | API Happy Path + 錯誤情境後端驗收 |
| TC-QUOTE-008~009 | BigDecimal 序列化格式（JSON 型別驗證） |
| TC-HISTORY-001~008 | History API 聚合邏輯、參數驗證、邊界值 |
| TC-FUND-001~005 | 基本面 API，包含 B-BE-W2-03 EPS 排序修正驗收 |
| TC-CHIP-001~006 | 籌碼 API，包含 totalNetBuySell 算術、buy/sell 缺陷記錄 |
| TC-ERR-5010~5014 | 後端錯誤碼觸發與 fallback 邏輯 |
| TC-CONTRACT-001~004 | Schema contract 欄位完整性（Postman assertion 專屬） |

### 效能（Quincy 主責）

JMeter `wave2_perf_baseline.jmx` 由 Quincy 獨立負責，Quinn 無對應案例，整合後無重疊。

---

## 8. 需要 Quinn 修正的項目（Approve-with-changes）

以下為要求修正後整合才能放行的項目：

### 必修（M1）：TC-Q-005-02 缺少 5014 「非重試」的斷言

**問題：** SPEC-Q-002 明確指出 5014 不應顯示重試按鈕，但 TC-Q-005-02 只驗證通知文字，沒有負向斷言。

**要求：**

```typescript
// 在 TC-Q-005-02 補充：
const descText = await page.locator('.ant-notification-notice-description').first().textContent();
expect(descText ?? '').not.toContain('請稍後重試');
expect(descText ?? '').not.toContain('Retry');
```

或補充「聯絡系統管理員」正向斷言驗證。

### 必修（M2）：TC-Q-008-03 超長 stockId 無實質斷言

**問題：** `expect(true).toBe(true)` 等同空測試，無法提供任何保護。

**要求：** 至少加入：

```typescript
// 頁面不應崩潰且應顯示 not-found 或跳轉
const url = page.url();
const hasErrorUI = await page.getByTestId('stock-not-found-result').isVisible({ timeout: 5_000 }).catch(() => false);
const isRedirected = !url.includes(longStockId);
expect(hasErrorUI || isRedirected, '頁面應回應 not-found 或跳轉，不應停留在原路徑').toBe(true);
```

### 必修（M3）：TC-Q-007-01 登入流程斷言被 `catch(() => {})` 吞掉

**問題：** `waitForURL().catch(() => {})` 讓測試永遠通過，無論登入跳轉是否成功。

**要求：** 若登入頁確實存在，登入後必須驗證跳轉發生；若測試環境不保證登入頁存在，應使用 `test.skip` 或在 beforeEach 先確認路由。

### 建議修正（S1）：TC-Q-010-01 的 table row selector 脆弱性

`chipCard.locator('td').filter(...)` 依賴 HTML 表格結構，建議補充 `data-testid="chip-institution-name"` 的需求給 Felix，或改用更穩健的 locator。

### 建議修正（S2）：TC-Q-003 重複的 mock body 可提取為 fixture

三個 stale tests 各自定義幾乎相同的 route mock，建議提取為：

```typescript
const mockStaleQuoteRoute = async (page: Page, traceId: string) => { ... };
```

---

## 9. 最終結論

**審查結果：Approve with Changes**

Quinn 的 E2E 測試成果整體品質高（8.8/10）。覆蓋面廣（12 TC / 37 case / 1119 行），在跨瀏覽器設定、selector 穩定性、測試隔離性上表現優秀。尤其 TC-Q-012（漲跌顏色）、TC-Q-003-03（DOM 位置）、TC-Q-004-06（traceId 透傳）是 Quincy 手動測試無法提供的精確驗證，屬高品質補充。

BUG-Q-001（i18n race condition）和 SPEC-Q-002（5014 重試按鈕語意）是有實用價值的發現，應建立 ticket 追蹤。

**阻止整合的問題（3 項必修）：**

1. TC-Q-005-02 缺少 5014「不顯示重試」斷言（SPEC-Q-002 驗收未落地）
2. TC-Q-008-03 超長 stockId 無實質斷言（`expect(true).toBe(true)` 無保護效果）
3. TC-Q-007-01 登入斷言被 `catch(() => {})` 吞掉，測試永遠通過

**Quinn 完成 M1/M2/M3 修正後，此 E2E 套件可納入整合測試清單，與 Quincy Postman API 測試 + JMeter 效能測試合併為 Wave 2 完整測試報告。**

---

## 附錄：整合後測試案例總數估計

| 類型 | 數量 | 主責 |
|------|------|------|
| API 功能測試（Postman） | 42（去重後保留所有 Quincy） | Quincy |
| E2E 自動化（Playwright） | 37（Quinn 全保留，Quincy 部分降為手動補充） | Quinn |
| 效能測試（JMeter） | 1 baseline scenario | Quincy |
| **整合後總案例數（不重複）** | **約 67**（去重約 12 項 UI 手動描述） | Quincy + Quinn |

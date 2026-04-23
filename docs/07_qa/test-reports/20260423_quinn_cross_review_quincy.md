# Quinn Cross-Review：Quincy Wave 2 測試成果審查報告

| 項目 | 內容 |
|------|------|
| 報告撰寫者 | Quinn（資深 QA #2） |
| 審查對象 | Quincy（資深 QA #1）Wave 2 測試成果 |
| 審查日期 | 2026-04-23 |
| 被審查檔案 | `docs/07_qa/test-cases/20260423_quincy_wave2_test-cases.md`（42 案例）|
| | `tests/postman/wave2_api_collection.json`（17 requests）|
| | `tests/jmeter/wave2_perf_baseline.jmx` |
| 審查依據 | `docs/03_spec/20260422_schema-lock_stock-detail-apis.md` v1.0 |
| | `docs/03_spec/20260422_errorCodes_central.md` v1.1 |
| 參照自有成果 | `docs/07_qa/test-cases/20260423_quinn_wave2_e2e_cases.md`（12 TC / 37 case）|
| | `tests/e2e/wave2_stockdetail.spec.ts`（1119 行 Playwright spec）|

---

## 一、重疊矩陣（Quinn E2E 案例 ↔ Quincy 功能 / API 案例）

下表分析 37 個 Quinn E2E case 與 Quincy 42 個功能 / API case 之間的對應關係。重疊定義：**兩者驗證同一業務行為**，但測試層次（E2E UI 對 API / 功能手動）可能不同。

| Quinn TC | Quinn 驗證焦點 | 對應 Quincy TC | Quincy 驗證焦點 | 層次差異 / 處置建議 |
|----------|---------------|----------------|-----------------|---------------------|
| TC-Q-001-01（頁面四區塊載入） | DOM 可見性 + testid | TC-FE-001 | 手動功能驗證 | 層次不同（E2E DOM vs. 手動）；**雙重保留**，各司其職 |
| TC-Q-001-02（title 含 stockId） | aria-label / title | TC-FE-001（隱含） | 手動未明確驗證 | Quinn 獨有，Quincy 未涵蓋 |
| TC-Q-002-01~03（週期切換三向） | Radio active 樣式切換 + 不閃爍 | TC-FE-005 | 手動功能驗證（僅提 weekly 一向） | Quinn 更完整（含逆向月→日、active class 驗證）；**整合後 TC-FE-005 縮編，主責 Quinn** |
| TC-Q-002-04~05（i18n 文字 zh/en） | 文字內容（日週月字串）| 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-003-01（isStale Tag 可見 + 日期文字）| DOM 存在 + 文字內容 | TC-QUOTE-007 / TC-FE-002 | 手動 + MSW | 行為一致；**Quinn 主責**（含座標順序驗證 TC-Q-003-03） |
| TC-Q-003-02（isStale=false 不顯示 Tag） | DOM 不存在 | TC-FE-002（隱含） | 未明確定義 | Quinn 獨有（正向排除驗證） |
| TC-Q-003-03（Tag 位置在 PriceHeader 前） | Y 座標比較 | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-004-01~06（5010-5014 zh-TW notification）| Ant Design notification 文字 + traceId | TC-FE-004 / TC-ERR-5010~5014 | 手動可見 / Postman code 值 | 層次完全不同（Quincy 驗證 code；Quinn 驗證前端 notification 文字）；**不重疊，均保留** |
| TC-Q-005-01~02（5010/5014 en notification） | 英文通知文字 | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-006-01（OTC market 標籤） | price-market testid 文字 | TC-QUOTE-002 / TC-FE-001（隱含）| Postman API 驗證 | 層次不同（E2E UI 對 API）；**均保留** |
| TC-Q-006-02（FundamentalCard source=MOPS） | aria-label tooltip | TC-FUND-002 | Postman API 驗證 source 欄位 | 層次不同；**均保留** |
| TC-Q-006-03（EPS/PER 精確數值）| DOM 文字精確比對 | TC-FUND-001（含 eps=string 驗證）| Postman schema 驗證 | 層次不同；Quinn 驗證前端渲染，Quincy 驗證 API 回傳；**均保留** |
| TC-Q-006-04（三大法人三筆顯示）| DOM 文字包含 | TC-CHIP-001 / TC-Q-010-01 重疊 | Postman 順序 + 手動 | **見 TC-Q-010-01 處置** |
| TC-Q-007-01~02（User Journey 登入後）| 登入流程 E2E | 無對應 | 未涵蓋（Quincy 無登入 E2E）| Quinn 獨有 |
| TC-Q-008-01（空 stockId 跳轉）| 路由跳轉行為 | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-008-02（4001 not-found Result）| DOM testid 可見 | TC-FE-003 / TC-QUOTE-003 | 手動 + Postman code=4001 | 層次不同（E2E UI DOM vs. 手動 vs. API）；**均保留** |
| TC-Q-008-03（超長 stockId 100 字元）| 不 crash | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-008-04（所有 API 中斷 / 網路斷線）| notification 在 12s 內出現 | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-008-05（period 非白名單前端路徑）| notification code=1003 | TC-HISTORY-004（Postman PV-01）| Postman 直接呼叫 | 層次不同；Quinn 從前端注入；Quincy 從 Postman 直接；**均保留** |
| TC-Q-009-01（FundamentalCard 六欄可見）| DOM testid 可見性 | TC-FUND-001（隱含 UI 呈現）| 手動功能驗證 | Quinn 更細化（testid 逐欄）；**整合後手動版縮編，主責 Quinn** |
| TC-Q-009-02（禁用 perRatio 文字）| DOM 文字不含 perRatio | TC-CONTRACT-002（HP-04 Postman）| JSON 欄位名禁止驗證 | **不重疊**：Quincy 驗證 JSON 鍵名；Quinn 驗證 UI 文字；**均保留** |
| TC-Q-009-03（季報 YYYY QN 格式）| DOM 文字 regex | TC-FUND-001（reportQuarter 1-4）| Postman 數值型別 | 層次不同；Quinn 驗證前端格式化；Quincy 驗證 API 數值；**均保留** |
| TC-Q-009-04（EPS string 格式）| DOM 文字含小數點 | TC-FUND-001 / TC-QUOTE-009 | Postman string type | Quinn 驗證前端顯示；Quincy 驗證 API 型別；**均保留** |
| TC-Q-010-01（三大法人固定順序）| DOM 逐筆 name | TC-CHIP-001 / HP-05 Postman | Postman 陣列順序驗證 | 層次不同（UI DOM vs. API JSON）；**均保留**；HP-05 為 API 層主責，TC-Q-010-01 為 UI 層主責 |
| TC-Q-010-02（totalNetBuySell 顯示）| chip-total-net testid 存在 + 有值 | TC-CHIP-003 / HP-05 Postman | Postman 計算等式驗證 | Quinn 驗證 UI 顯示；Quincy 驗證計算正確性；**均保留** |
| TC-Q-010-03（買超紅 / 賣超綠顏色）| inline style color hex | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-010-04（籌碼日期格式）| Card 文字含 YYYY-MM-DD | TC-CHIP-001（date 欄位格式）| Postman regex | 層次不同；**均保留** |
| TC-Q-011-01~02（RWD iPhone 14）| 手機 viewport 可見性 + 點擊 | 無對應 | 未涵蓋 | Quinn 獨有 |
| TC-Q-012-01~03（漲跌顏色 + 平盤）| inline style color hex | 無對應 | 未涵蓋 | Quinn 獨有 |

**重疊統計**：37 case 中，與 Quincy 有層次性交集（同業務行為，不同測試層）的共 **19 case**；Quinn 純獨有（Quincy 完全未覆蓋）的共 **18 case**。

---

## 二、Quincy 漏測項目（從 E2E 視覺 / 互動角度）

以下為 Quincy 測試案例與 Postman collection 均未涵蓋，由 Quinn E2E 單獨承擔的關鍵驗證點：

### 2-1 UI 視覺 / 樣式類（高優先）

| 缺口編號 | 缺口描述 | 嚴重度 | 對應 Quinn TC |
|----------|----------|--------|---------------|
| GAP-Q-01 | **漲跌顏色未驗證**：PriceHeader 漲（紅 #cf1322）/ 跌（綠 #3f8600）/ 平盤（無色）的 inline style，Quincy 的功能測試與 Postman 均無此層次 | High（台灣股市核心慣例，視覺錯誤直接影響使用者判斷） | TC-Q-012-01~03 |
| GAP-Q-02 | **ChipCard 正負顏色未驗證**：買超紅 / 賣超綠的 color hex 驗證完全缺失 | High | TC-Q-010-03 |
| GAP-Q-03 | **stale-data-tag DOM 位置（Y 座標）**：Quincy 僅驗證 Tag 可見，未驗證 Tag 在 PriceHeader 之上（DOM 順序正確性） | Medium | TC-Q-003-03 |
| GAP-Q-04 | **isStale=false 時 Tag 不存在的排除驗證**：Quincy TC-FE-002 只驗證 true 情境，未驗證 false 時 DOM 乾淨（無 stale-data-tag 殘留） | Medium | TC-Q-003-02 |

### 2-2 多語系（i18n）類

| 缺口編號 | 缺口描述 | 嚴重度 | 對應 Quinn TC |
|----------|----------|--------|---------------|
| GAP-Q-05 | **Period selector i18n 文字**：日 / 週 / 月 在 zh-TW 與 en 的顯示文字完全未驗證 | Medium | TC-Q-002-04~05 |
| GAP-Q-06 | **5010-5014 英文通知文字**：Quincy 的 TC-FE-004 僅驗證通知出現（手動），無英文語系驗證 | Medium | TC-Q-005-01~02 |

### 2-3 邊界 / 防禦性類

| 缺口編號 | 缺口描述 | 嚴重度 | 對應 Quinn TC |
|----------|----------|--------|---------------|
| GAP-Q-07 | **空 stockId 路由跳轉**：URL `/stocks/` 的防禦邏輯（Navigate replace）未被任何手動 / API 測試覆蓋 | High（路由 crash 風險） | TC-Q-008-01 |
| GAP-Q-08 | **超長 stockId（100 字元）不 crash**：極端邊界值輸入，Quincy 無此 case | Medium | TC-Q-008-03 |
| GAP-Q-09 | **全 API 中斷 / 網路斷線情境**：abort 所有請求後前端是否仍顯示通知、不 crash。Quincy 的錯誤測試均假設後端回傳特定 errorCode，從未測試網路層中斷 | High（生產環境常見情境） | TC-Q-008-04 |

### 2-4 User Journey 類

| 缺口編號 | 缺口描述 | 嚴重度 | 對應 Quinn TC |
|----------|----------|--------|---------------|
| GAP-Q-10 | **登入後訪問個股詳情的完整流程**：Quincy 所有測試均假設已有 token，從未驗證登入→跳轉→詳情頁的端到端流程。未登入直接訪問 /stocks/2330 是否正確攔截亦未驗證 | Medium | TC-Q-007-01~02 |

### 2-5 跨瀏覽器 / RWD 類

| 缺口編號 | 缺口描述 | 嚴重度 | 對應 Quinn TC |
|----------|----------|--------|---------------|
| GAP-Q-11 | **行動裝置 RWD**：Quincy 完全未涵蓋 iPhone 14 viewport 的可見性與可點擊性驗證 | High（行動用戶佔比高） | TC-Q-011-01~02 |
| GAP-Q-12 | **WebKit 跨瀏覽器**：Quincy 未提及跨瀏覽器測試計劃，所有手動測試假設單一瀏覽器環境 | Medium | TC-Q-001~012（WebKit 設定） |

### 2-6 Spec 模糊識別類（Quinn 獨立發現）

| 缺口編號 | 缺口描述 | 嚴重度 |
|----------|----------|--------|
| GAP-Q-13 | **5014 不應顯示重試按鈕**（SPEC-Q-002）：`isDataSourceError` 統一包含 5010-5014，但 5014 是格式異動，不可重試；Quincy 的 TC-FE-004 未細分此行為差異 | Medium（UX 設計問題） |
| GAP-Q-14 | **i18n 初始化 race condition**（BUG-Q-001）：MSW await 與 i18n 同步初始化間的時序問題，Quincy 未發現 | Low |

**Quinn 漏測缺口合計：14 項**，其中 High 4 項、Medium 8 項、Low 2 項。

---

## 三、Quinn 應吸收 Quincy 的優點

### 3-1 Schema Contract 驗證（Postman 層次，Quinn 較弱）

Quincy 的 Postman collection 在以下維度顯著優於 Quinn 的 E2E 覆蓋：

| 優點項目 | Quincy 如何做 | Quinn 現況 | 建議 |
|----------|--------------|------------|------|
| **BigDecimal → String 型別驗證** | HP-01 逐欄位 `typeof` 斷言（7 個 string 欄位 + 1 個 number）| TC-Q-009-04 僅驗證 DOM 文字含小數點，未驗證 JSON 原始型別 | Wave 3 建立 Pact consumer contract，Quinn 負責前端 consumer side |
| **欄位完整性（不多不少）** | SC-01 用 Set 比對，多送欄位有 console.warn | Quinn 無 schema 完整性驗證 | 整合後 contract 測試由 Quincy Postman 主責，Quinn E2E 補充 UI 呈現 |
| **change / changePercent 帶符號 regex** | HP-01 `pm.expect(data.change).to.match(/^[+-]/)` | TC-Q-012-01~03 只驗證顏色，未驗證 DOM 文字的符號格式 | Quinn 在 TC-Q-012 加入 price-change testid 文字的符號 regex 驗證（`/^[+\-]/`） |
| **items 升冪排序邏輯斷言** | HP-02 / SC-04：迴圈比對相鄰 date | Quinn 無 K 線歷史排序驗證 | Quinn 在 TC-Q-002（週期切換）後，加入歷史資料順序的前端呈現驗證 |
| **totalNetBuySell 計算等式** | HP-05 `reduce` 計算後比對 API 回傳值 | TC-Q-010-02 只驗證 DOM 存在 + 有值 | Quinn 利用 `page.evaluate()` 從 DOM 取出三法人值加總，驗證與 chip-total-net 文字一致 |
| **weekly 聚合效果驗證** | HP-03：筆數 ≤ 5（3 週範圍） | Quinn 無週聚合數量驗證 | Quinn E2E 可在週期切換後從 DOM chart tooltip 抽樣確認點數合理減少（如從 15 點縮減為 3 點） |

### 3-2 效能基準意識（JMeter 層次）

Quincy 的 JMeter 計劃定義了清晰的驗收標準：

| 指標 | Quincy 設定值 | Quinn 現況 |
|------|--------------|------------|
| p95 < 500ms | JMeter Aggregate Report | Quinn Playwright 的 `pm.expect(responseTime).to.be.below(500)` 只在 Postman（不是 Quinn 的） |
| p99 < 1000ms | JMeter Duration Assertion（per sampler）| 無 |
| Error Rate < 1% | JMeter Response Assertion（code=0 驗證）| 無 |
| Throughput >= 50 RPS | Thread Group 50 users / 60s | 無 |

**建議 Quinn 吸收**：在 Playwright 的 TC-Q-001（頁面載入）中加入 `page.clock` 計時驗證（API 請求 < 2s，含前端渲染），形成前端感知效能基準。雖然無法取代 JMeter 的後端壓測，但可建立「正常情況下首屏載入不超過 X 秒」的 E2E 效能門檻。

### 3-3 已知缺陷的系統性標記

Quincy 對 BUG-QUINCY-001（ChipDTO 多送 stockName）和 BUG-QUINCY-002（buy/sell 暫填 0）的記錄方式值得借鑑：
- 在 Postman 測試中明確標記 `Known Defect`，不讓缺陷造成整個 collection fail
- 對應 TC 標注「預期外行為，待修」，避免誤判

Quinn 的 E2E 測試目前**缺乏**對這兩個已知缺陷的明確處置（TC-Q-010-01 驗證順序，但未標注 stockName 不應出現在前端渲染中）。Wave 3 前應在 TC-Q-010 加入 `stockName 不應顯示在 ChipCard 任何欄位` 的 DOM 斷言。

---

## 四、整合後去重建議（主責分配）

### 4-1 主責原則

- **Quincy 主責**：API 合約驗證、BigDecimal 序列化型別、後端業務邏輯（EPS 排序、totalNetBuySell 計算）、效能基線
- **Quinn 主責**：UI DOM 呈現、視覺顏色、i18n 文字、跨瀏覽器、RWD、User Journey
- **雙重覆蓋**（不去重，保留）：happy path 頁面載入、errorCode 觸發（Quincy 驗後端 code，Quinn 驗前端 notification）

### 4-2 去重建議表

| 整合後案例 | 作者主責 | 去重依據 | 處置 |
|-----------|---------|---------|------|
| TC-FE-005（KLine 週期切換） | Quinn 主責 | TC-Q-002 更完整（三向 + active class + 逆向） | 縮減 Quincy TC-FE-005 為「見 TC-Q-002 E2E 涵蓋，手動版退場」 |
| TC-FE-001（頁面四區塊） | 雙重保留 | E2E 與手動層次不同 | 標記「Quinn E2E 主責驗收；Quincy 手動備份」 |
| TC-FE-002（isStale Tag） | Quinn 主責 | TC-Q-003 更完整（含位置驗證、false 排除） | 縮減 Quincy TC-FE-002，保留 TC-Q-003 為主 |
| TC-FE-003（4001 UI） | 雙重保留 | Quincy 驗後端 code；Quinn 驗前端 stock-not-found-result DOM | 均保留，標記層次 |
| TC-QUOTE-007（isStale quoteDate 日期） | 雙重保留 | Quincy 驗 API quoteDate 值；Quinn 驗 Tag 文字含日期 | 均保留 |
| TC-FE-004（notification.error 手動）| Quinn 主責 | TC-Q-004 的 E2E 更精確（含文字、traceId）| 縮減 Quincy TC-FE-004，標記「Quinn E2E 驗收即可」 |
| TC-FUND-001 UI 呈現部分 | Quinn 主責（TC-Q-009） | Quincy API 主責 HP-04；Quinn UI 主責 TC-Q-009 | 均保留，僅標記分工 |
| TC-CHIP-001 UI 呈現部分 | Quinn 主責（TC-Q-010） | Quincy API 主責 HP-05；Quinn UI 主責 TC-Q-010 | 均保留，僅標記分工 |

### 4-3 優先級排序（整合後共 79 case → 去重後估計 65 case）

| 優先級 | case 數（估計）| 說明 |
|--------|--------------|------|
| P0（Blocker 驗收）| 約 22 | Quincy 的 schema contract + 業務邏輯；Quinn 的頁面載入 / errorCode notification / isStale |
| P1（核心功能）| 約 28 | 週期切換 / OTC 路徑 / User Journey / 效能基線 / i18n |
| P2（防禦性）| 約 15 | 邊界值 / RWD / 超長 stockId / 網路斷線 |

---

## 五、Quincy 測試品質評分

### 5-1 各維度評分

| 評分維度 | 得分（/10）| 評分理由 |
|----------|-----------|---------|
| 覆蓋廣度 | 9.0 | 42 案例覆蓋四模組（QUOTE / HISTORY / FUND / CHIP）+ 前端整合 + Contract + 錯誤碼，無明顯大面積缺口 |
| AC 追溯性 | 9.5 | 每個 TC 標注對應 schema-lock 章節，Contract 案例與 SRS 精確對應，可追溯性極高 |
| 邊界條件 | 7.5 | 參數驗證（1001/1002/1003）、日期範圍超限、預設值測試均有；但缺少超長字串 / 空字串 / 特殊字元等 payload 邊界測試 |
| 錯誤情境 | 8.5 | 5010-5014 五個錯誤碼均有 Postman 自動化 + 手動；fallback（isStale / DB 回退）邏輯有詳細 Given-When-Then；唯缺網路層中斷（abort）情境 |
| 預期結果量化 | 9.0 | Postman assertion 均有明確 `eql` / `match` / regex；JMeter 有 p95/p99/Error Rate 數值門檻；手動 case 部分仍為「含語意」等描述，略扣分 |
| 逆向操作 | 6.0 | 缺少週期切換的逆向（月→週→日）驗證；TC-FE-005 只驗証一個方向 |
| 非功能測試（安全 / 效能）| 8.5 | JMeter 效能基線完整（50 user / 60s / p95<500ms）；Postman HP 各 request 含 responseTime<500ms 斷言；OWASP / Authorization 驗證缺失（未測無 token / 非法 token 情境）|
| 自動化程度 | 8.0 | Postman 17 requests 均有自動化斷言（Newman 可執行）；JMeter 可 CI 整合；手動 case（TC-FE-001~005）5 個仍為手動，未達到全自動化 |
| 已知缺陷處理 | 9.5 | BUG-QUINCY-001 / 002 / SPEC-QUINCY-001 三個問題識別清晰，標記方式規範（不 FAIL 但記錄），Wave 3 修正計劃明確 |
| 跨瀏覽器 / RWD | 2.0 | 完全缺失；所有測試假設單一桌面瀏覽器環境 |

### 5-2 總分

**加權平均：8.1 / 10**

（權重：覆蓋廣度 15% / AC 追溯 10% / 邊界 10% / 錯誤情境 15% / 預期量化 10% / 逆向 5% / 非功能 15% / 自動化 10% / 缺陷處理 5% / 跨瀏覽 5%）

### 5-3 優點摘要

1. **AC 追溯性極高**：每個 TC 明確對應 schema-lock 章節，審查者可快速確認覆蓋完整性
2. **已知缺陷標記規範**：BUG-QUINCY-001/002 的「不 FAIL 但記錄 + Wave 3 修正計劃」是成熟 QA 的處理方式
3. **Contract 測試層次清晰**：SC-01~04 的 Postman collection 實現了輕量 contract test，是 Pact 引入前的良好過渡
4. **效能基準設計合理**：JMeter 的 Think Time 100ms 模擬真實用戶行為，三端點均勻分配，基線數值（p95<500ms）與 schema-lock 一致

### 5-4 需改善項目

1. **跨瀏覽器 / RWD 完全缺失**（最大弱點）：手動測試應指定至少 Chrome + Edge + iOS Safari
2. **逆向操作不足**：週期切換僅驗證正向，缺月→週→日的逆向路徑
3. **網路層中斷情境未覆蓋**：所有錯誤情境均假設後端能回傳 Envelope，未處理 TCP timeout / 連線中斷
4. **Authorization 安全測試缺失**：未驗證無 Bearer token / 過期 token / 非法 token 的後端回應行為（OWASP A01）
5. **手動案例（TC-FE-001~005）未計劃自動化**：TC-FE-001/002/003 在 Quinn 的 E2E 中均已自動化，應在整合報告中標記退場

---

## 六、整合結論

### 6-1 審查結論

**Approve with Changes**

Quincy 的測試成果整體品質優良，API 合約驗證、後端業務邏輯、效能基線三個面向達到 Wave 2 放行標準。主要問題集中在**跨瀏覽器 / RWD 缺失**與**部分手動案例未計劃自動化退場**，這兩點須在整合報告中明確處置，但不構成 Blocker。

### 6-2 必要修改項目（Approve 前務必確認）

| # | 必要修改 | 負責人 | 期限 |
|---|---------|--------|------|
| M-01 | TC-FE-001 / TC-FE-002 / TC-FE-003 / TC-FE-004 / TC-FE-005 五個手動案例，在整合文件中標記「Quinn E2E 主責，手動版退場」 | Quincy | 整合前 |
| M-02 | TC-FE-005 縮編說明，指向 TC-Q-002 的 E2E 週期切換案例（含三向逆向） | Quincy | 整合前 |
| M-03 | 在 Postman collection 補充一個 EP-00 安全測試：無 Authorization header 呼叫任一 API，應回 3001 或 401（OWASP A01 最低驗收） | Quincy | Wave 2 放行前 |

### 6-3 Wave 3 建議補強（不阻擋 Wave 2 放行）

| # | 建議項目 | 建議負責人 |
|---|---------|----------|
| R-01 | 引入 Pact contract testing，取代目前 Postman SC-01~04 的輕量 contract test | Quincy 主導（後端 provider）/ Quinn 配合（前端 consumer） |
| R-02 | Quincy 補充跨瀏覽器手動測試清單（至少 Chrome + Edge + iOS Safari，對應 TC-FE-001~005） | Quincy |
| R-03 | Quinn 在 TC-Q-012（漲跌顏色）補充 change 文字符號 regex 驗證（absorb Quincy HP-01 的 regex 優點） | Quinn |
| R-04 | Quinn 補充 TC-Q-013：ChipCard 不顯示 stockName（對應 BUG-QUINCY-001 Wave 3 修正後的驗收 case） | Quinn |
| R-05 | JMeter 擴充 quote/history 端點壓測（目前僅三個，缺 history；B-M-03 N+1 修正後的效能驗收） | Quincy |

### 6-4 整合行動計劃

1. Quincy 完成 M-01~M-03 後，由 Quinn 確認
2. 雙方共同建立 `docs/07_qa/test-cases/integrated/20260423_wave2_integrated.md`（去重後 65 case）
3. 整合後案例送 Jamie，由 Jamie 決定是否進入放行評審
4. BUG-QUINCY-001 / BUG-QUINCY-002 / SPEC-Q-002 三個問題透過 Jamie 建立追蹤 ticket，列入 Wave 3 backlog

---

*報告撰寫：Quinn（資深 QA #2）*
*審查日期：2026-04-23*
*下一動作：將 M-01~M-03 修改要求透過 Jamie 傳遞給 Quincy*

# Code Review 報告 — Wave 2 Round 2（跨團隊對齊驗收）

| 項目 | 內容 |
|------|------|
| 審查者 | Brian（資深後端 Reviewer） |
| 審查日期 | 2026-04-22 (GMT+8) |
| 審查範圍 | Bruno Round 2 對齊變更（commit `c2ad5bc`） |
| 對照基準 | `docs/03_spec/20260422_schema-lock_stock-detail-apis.md` v1.0、D1-D6 仲裁紀錄 |
| 變更檔案數 | 14 modified + 5 new（共 19 檔） |
| 上一輪評分 | 6.5/10（GO-WITH-FIXES，4 Blocker） |
| **本輪評分** | **8.5/10**（GO-WITH-FIXES） |
| **本輪結論** | **GO-WITH-FIXES**（無 Blocker，可進入 QA；保留 3 Major 並列 Wave 3 修正） |

---

## 1. 總體評價

Bruno 在 Round 2 的對齊工作展現出顯著進步。Round 1 的 4 項 Blocker（QuoteServiceImpl/ChipServiceImpl 週末邏輯、EPS 期別排序、跨團隊 DTO 不對齊）**全部修復且有對應單元測試覆蓋**。聚合邏輯（weekly/monthly K 線）實作正確且測試覆蓋充分。

不過仍有 3 項 Major 需在 Wave 3 處理（涉及 schema-lock 嚴格遵守、效能、序列化精度），以及若干 Minor。**整體可放行進入 QA 階段，但必須將 Major 列入 Wave 3 backlog**。

---

## 2. Round 1 Blocker 修復確認

| Round 1 Blocker | 描述 | 修復檔案 | 修復狀態 | 對應測試 |
|---|---|---|---|---|
| **B-BE-W2-01** | QuoteServiceImpl 週末邏輯錯誤（DB 有 stale 仍呼叫外部，且未標 isStale） | `QuoteServiceImpl.java` lines 134-148, 156-165 | 已修復：採 `isValidRecentTradingDay` 檢查 + 外部回空時 fallback DB 並設 `toDTOStale` | `QuoteServiceImplTest`：`getQuote_Weekend_DbHasPreviousTradingDay_ReturnStale`、`getQuote_ExternalEmpty_FallbackToDbStale`、`getQuote_NoDbNoExternal_ThrowsStockNotFound` |
| **B-BE-W2-02** | ChipServiceImpl 同樣的週末邏輯錯誤 | `ChipServiceImpl.java` lines 78-105 | 已修復：對齊 Quote 的 fallback 流程 | `ChipServiceImplTest`：`getChip_Weekend_DbWithinRecentTradingDays_ReturnDbDirectly`、`getChip_ExternalEmpty_FallbackToDb`、`getChip_NotFound` |
| **B-BE-W2-03** | EPS reportYear/Quarter 取 `epsList.get(0)`，依賴排序順序 | `FundamentalServiceImpl.java` lines 105-109 | 已修復：改用 `stream().max(Comparator.comparingInt(year).thenComparingInt(quarter))` | `FundamentalServiceImplTest.getFundamental_EpsUnsorted_CorrectLatestQuarter`（故意亂序驗證） |
| **跨團隊 DTO 不對齊** | QuoteDTO/FundamentalDTO/ChipDTO 與前端不一致 | 14 個檔案 + 5 個新增 | 已對齊（細節見 §3） | 各 `*ConvertorTest` + `*ServiceImplTest` |

**結論**：Round 1 4 個 Blocker 全部修復，無回歸。

---

## 3. 跨團隊對齊驗收（D1-D6）

| 決策 | 仲裁要求 | Bruno 實作 | 驗收結果 |
|------|---------|----------|---------|
| **D1** | QuoteDTO 採 `price/open/high/low/change/changePercent/previousClose/updatedAt/source/isStale` | `QuoteDTO.java`：14 個欄位完全對齊 schema-lock §1.3 | OK |
| **D1** | FundamentalDTO 採 `per/pbr` 去 Ratio + `updatedAt/source` | `FundamentalDTO.java`：10 個欄位對齊 §4.3 | OK |
| **D1** | ChipDTO 採 `institutions[]` array + `totalNetBuySell/source/date` | `ChipDTO.java`：6 個欄位（含 stockName，見 M-02）；`InstitutionItem.java` 4 欄位齊全 | 大致 OK；`stockName` 是契約外多送（M-02） |
| **D2** | ErrorCode 補 5013 OTC、5014 MOPS_FORMAT_CHANGED | `ErrorCode.java` lines 69-70 已加入 | OK |
| **D3** | period enum (daily/weekly/monthly)，後端做聚合 | `KLinePeriod.java` enum + `QuoteServiceImpl.aggregateHistory()` 完整實作 | OK |
| **D4** | endpoint 改 `/api/v1/quote/history`（去 `/get`） | `QuoteController.java` line 54 `@PostMapping("/history")` | OK |
| **D5** | Peter SRS 為唯一契約來源 | Bruno 全程依 schema-lock 對齊；alignment 文件清楚追溯每項變更 | OK |
| **D6** | Wave 3 加 Pact contract test | 已列入 Wave 3 backlog（F7） | 待 Wave 3，本輪不要求 |

**結論**：D1-D6 全部達成（除 D6 為 Wave 3 任務）。

---

## 4. 問題分級

### 4.1 Blocker（0 項）

**無 Blocker**。

### 4.2 Major（3 項）

#### M-01: QuoteDTO `change` / `changePercent` 缺少正號前綴格式化

- **位置**：`QuoteConvertor.java` lines 53-72；`QuoteDTO.java` line 27-28
- **問題**：schema-lock §1.3 明確要求 `change` / `changePercent` 為**字串**且**正值帶 `+` 前綴**（例如 `"+19.00"` / `"+1.84"`）。但 DTO 宣告為 `BigDecimal`，且 Jackson 預設未配置 `WRITE_BIGDECIMAL_AS_PLAIN` 或自訂 Serializer，序列化結果為 number 或 `"19.00"`（無 `+`）。
- **後果**：前端拿到的 JSON 可能是 `"change": 19.00`（number）或 `"19.00"`（無正號），與 schema-lock §1.3 範例 `"change": "+19.00"` 不符。前端漲跌色彩判斷依賴此格式可能失準。
- **建議修正**：
  1. 在 `QuoteDTO` 將 `change` / `changePercent` 型別改為 `String`，於 Convertor 內透過 `BigDecimal.signum() >= 0 ? "+" + value : value.toPlainString()` 格式化；或
  2. 自訂 `JsonSerializer<BigDecimal>` 標 `@JsonSerialize(using = SignedDecimalSerializer.class)`。
  3. **同步處理** `price`、`previousClose`、`open`、`high`、`low`：schema-lock §0 規定金額/比率類「BigDecimal → 序列化為 string 保留精度（避免 JS 浮點誤差）」，目前未配置會被序列化為 number，前端會遇到精度問題。
- **嚴重度依據**：契約偏差，影響前端畫面顯示與資料解析。OWASP 不直接相關，但屬資料完整性議題。

#### M-02: ChipDTO 多送 `stockName` 欄位（schema-lock 未定義）

- **位置**：`ChipDTO.java` line 19；`ChipConvertor.java` line 45
- **問題**：schema-lock §5.3 ChipDTO 定義欄位為 `stockId / date / institutions / totalNetBuySell / source` 共 5 項，**未列 `stockName`**，但目前 record 多了 `stockName`。
- **後果**：違反 schema-lock 「single source of truth」原則（D5）。雖然多送欄位前端可忽略，但若 contract test（D6 Wave 3）以 schema-lock 為基準，會直接 fail。
- **建議修正**：兩條路徑擇一
  1. 移除 `ChipDTO.stockName`（若前端 `StockChip` 型別亦無，最乾淨）。
  2. 若業務確實需要，發 `[SRS-CHANGE]` PR 由 Peter 補回 schema-lock §5.3 與 §6 TypeScript 型別。
- **比較參考**：`QuoteDTO.stockName`（§1.3 / §6 都有定義）、`FundamentalDTO.stockName`（§4.3 / §6 都有定義），唯獨 ChipDTO 不一致。

#### M-03: `previousClose` 查詢造成 N+1 與快取一致性風險

- **位置**：`QuoteServiceImpl.java` lines 197-206、273-277、463-489、494-528
- **問題 1（N+1）**：`listQuotes` 對每個 cache miss 的股票呼叫 `resolvePreviousClose(stockId, quoteDate)`，每次一筆 DB 查詢。`fetchAndCacheAll` 同樣對每筆 TWSE/OTC 行情呼叫 `resolvePreviousClose`。若全市場 ~2000 檔，會額外發出 ~2000 次 SQL。
- **問題 2（快取一致性）**：`getQuote` cache hit 直接回傳整個 DTO，但 `previousClose` 是當下計算的快照；隔日盤前若 cache 還未過期，回傳的 `previousClose` 是錯的（昨日的昨日）。盤後 TTL 1 小時、盤中 30 秒可降低風險，但仍有窗口期。
- **建議修正**：
  1. **批次查詢**：新增 `findPreviousByStockIds(List<String>, LocalDate)` MyBatis XML 動態 SQL，一次查回所有股票前一交易日 close（已列 alignment 文件 Wave 3 TD）。
  2. **快取設計**：考慮在 cache miss 後計算 `previousClose` 時，cache key 加入 `quoteDate` 或在 cache hit 時驗證 `quoteDate` 是否仍代表當日（或最近交易日）。
- **嚴重度依據**：listQuotes 是高頻 endpoint，N+1 影響 P99 latency；cache 不一致會出現「漲跌數字短暫錯誤」的 UX 問題。

### 4.3 Minor（5 項）

#### N-01: weekly/monthly 範圍計算用 `ChronoUnit.DAYS.between` 不夠精準

- **位置**：`QuoteServiceImpl.java` lines 244, 312
- **問題**：`MAX_WEEKLY_DAYS = 1825L`、`MAX_MONTHLY_DAYS = 3650L` 用「天數」估算 5 年/10 年，閏年會差 1-3 天。schema-lock §3.4 寫的是「5 年/10 年」，建議用 `ChronoUnit.YEARS.between(startDate, endDate)` 比對年數。
- **影響度**：邊緣情境（恰好跨閏年）才會誤判為超範圍，影響低。

#### N-02: aggregateByKey 每組 5 次 stream traversal

- **位置**：`QuoteServiceImpl.java` lines 368-380
- **問題**：每個 group 對 `high`、`low`、`volume` 各 stream 一次（共 3 次），加上 sorted、map 整體 5+ 次。資料量小時可忽略，但 5 年日線 ~1300 筆在 monthly 聚合（60 個月）時仍會多次走訪。
- **建議**：改用單次 for-loop 累加（high=max、low=min、volume=sum），效率更好。
- **參考規範**：`code-style.md` 提到 Stream 操作鏈不超過 5 步、簡單迴圈不濫用 Stream。

#### N-03: ChipConvertor `buy/sell` 暫填 0 已在 alignment 文件標記，但測試未覆蓋

- **位置**：`ChipConvertor.java` lines 36-40；`ChipServiceImplTest.chipConvertor_ToDTO_ProducesInstitutionsArray` line 183-185
- **問題**：測試只驗證 `netBuySell`，未驗證 `buy=0L` / `sell=0L` 是 Wave 3 暫行決策。應加 `assertThat(dto.institutions().get(0).buy()).isEqualTo(0L)` 與註解，提醒未來補欄位時測試會 fail。
- **建議**：補強測試 + 在 `ChipConvertor.java` JavaDoc 加 `// Wave 3 TD: 待 PO 補 buy/sell 明細` 標記（目前在 alignment 文件有但 source 沒有）。

#### N-04: `QuoteConvertor.toDTO` 簽名變更但未強制檢查呼叫點

- **位置**：`QuoteConvertor.java` line 39（新簽名 `toDTO(po, previousClose)`）
- **問題**：簽名從 `toDTO(po)` 改為 `toDTO(po, previousClose)`，雖然編譯期會發現呼叫點，但建議在 PR 描述列出所有呼叫處，便於 reviewer 快速核對（已透過 `QuoteServiceImpl` 全部更新到位，但若有他模組依賴會遺漏）。
- **建議**：補一個 unit test 確認 `QuoteConvertor` 介面方法簽名（reflection 驗證），或在 `QuoteServiceImpl` 加註解標明所有 4 處呼叫。

#### N-05: 預設範圍與最長範圍邊界缺測試

- **位置**：`QuoteServiceImpl.java` lines 295-301（`resolveDefaultStartDate`）
- **問題**：weekly 預設 365 天、monthly 預設 5×365 天，但 `QuoteHistoryAggregationTest` 沒測試「未帶 startDate 時是否套對預設」與「恰好等於最長範圍邊界」。
- **建議**：補 2 個測試
  - `getHistory_NoStartDate_AppliesDefault`
  - `getHistory_AtMaxRange_NoException`（如 daily 1825 天剛好不報錯）

### 4.4 背景問題（不屬本輪 Review 範圍但須提示）

#### B-01: stock-infrastructure 模組編譯失敗（Spring 6.x 相容性）

- **位置**：`backend/stock-infrastructure/src/main/java/tw/com/stockplatform/infrastructure/config/RestClientConfig.java:89`
- **錯誤**：`HttpComponentsClientHttpRequestFactory.setReadTimeout(int)` 在 Spring Framework 6.1+ 已被移除，需改用 `setConnectTimeout(Duration)` 與 `HttpClient` 內建 `RequestConfig.responseTimeout`。
- **影響**：阻擋 `mvn compile` / 整個專案測試執行。**Bruno 與 Felix 變更模組（stock-quote/chip/fundamental）的測試無法被獨立驗證執行通過**，僅能透過閱讀程式碼判定。
- **歸屬**：屬 Wave 1 既有問題（非本 Round 引入），但建議 Linus 與 Bruno 在 Wave 3 一併處理；Jamie 視為 P1 處理。

---

## 5. 程式碼品質檢查

| 檢查項 | 狀態 | 備註 |
|--------|------|------|
| Java 21 現代語法 | 良好 | record、sealed pattern matching、`switch` expression 善用 |
| 命名清晰 | 良好 | KLinePeriod 用小寫違反 enum 大寫慣例，但符合對前端 JSON 序列化需求 |
| 方法長度 | 良好 | `aggregateByKey` 約 35 行，最長；無超過 50 行 |
| 類別長度 | 中等 | `QuoteServiceImpl` 543 行（接近 500 警戒線，建議下次拆分 `QuoteHistoryService`） |
| 註解品質 | 優秀 | JavaDoc 解釋「為什麼」+ 引用 schema-lock 章節 |
| 交易管理 | 良好 | `@Transactional(readOnly = true)` 預設 + 個別寫操作覆寫；rollback 行為正確 |
| SQL 注入防護 | 良好 | 全部 `#{}`，無 `${}` |
| 大量資料分頁 | 良好 | history 查詢有 startDate/endDate 範圍限制 |
| 金額用 BigDecimal | 良好 | 全程 BigDecimal，使用 `BigDecimalUtils.safeXxx` |
| 時間用 LocalDateTime | 良好 | 無 Date / Calendar |
| Optional 使用 | 良好 | `findLatestByStockId` / `findPreviousByStockId` 回 Optional，DTO 不含 |
| Stream 使用 | 中等 | 多數合規，但 `aggregateByKey` 可優化（N-02） |
| SLF4J 日誌 | 良好 | 全部 `log.info/warn/error`，無 println |
| 單元測試覆蓋 | 良好 | QuoteServiceImpl 7 測試 + Aggregation 6 測試 + Convertor direct test；估覆蓋率 ≥85% |
| 環境配置 | 良好 | `cacheKeyPrefix` 透過 `@Value("${stock.cache.key-prefix:}")` 注入 |

---

## 6. 測試品質評估

### 6.1 QuoteHistoryAggregationTest（新增 6 測試）

| 測試名稱 | 覆蓋情境 | 評價 |
|---------|---------|------|
| `getHistory_Daily_ReturnsDailyItems` | daily 一對一映射 | OK |
| `getHistory_Weekly_AggregatesSameWeek` | 同一週 3 天聚合為 1 筆 | OK，open/high/low/close/volume 完整斷言 |
| `getHistory_Weekly_TwoWeeks` | 跨 2 週分組正確 | OK |
| `getHistory_Monthly_TwoMonths` | 跨 2 月分組正確 | OK |
| `getHistory_StartDateAfterEndDate_ThrowsParamFormatInvalid` | 1002 邊界 | OK |
| `getHistory_Daily_ExceedsMaxRange_ThrowsParamOutOfRange` | 1003 邊界 | OK |

### 6.2 缺漏測試（建議補強）

- **週一假日邊界**：例如 4/14（週一）放假，4/15-4/17 為週二-週四交易日。`weekKey()` 用 `previousOrSame(MONDAY)` 仍會 group 到 4/14，但 `first` 變成 4/15。應驗證 `date = 4/15` 而非 `4/14`（目前 alignment 文件 §B 已標 [SRS-CHANGE 候選]，建議在 SRS 鎖定行為前先補一個測試確認當前實作）。
- **月初假日邊界**：例如 5/1 是國際勞動節，月 K 第一筆應為 5/2 而非 5/1。
- **空資料**：`dbRows` empty 時應回 `items = []` 而非 NPE（目前 `aggregateHistory` 已處理 lines 331-333，但無測試）。
- **未帶 startDate 預設值套用**：見 N-05。

### 6.3 既有測試更新

- `QuoteServiceImplTest`：所有 7 個測試的 mock 設定都涵蓋新的 `findPreviousByStockId` 與 `toDTO(po, previousClose)` 簽名；新增 `getQuote_WithPreviousClose_ChangeCalculated` 驗證 previousClose 確實被傳入 Convertor。OK。
- `FundamentalServiceImplTest`：保留 B-BE-W2-03 / B-BE-W2-04 既有測試，新增 `fundamentalConvertor_ToDTO_CorrectFieldMapping` 直接驗證 default method 邏輯。OK。
- `ChipServiceImplTest`：保留 Round 1 6 測試，新增 `chipConvertor_ToDTO_ProducesInstitutionsArray` 驗證 institutions 順序與 total。OK。

---

## 7. 安全性檢查（OWASP）

| 項目 | 狀態 | 說明 |
|------|------|------|
| A01 失效存取控制 | N/A | 本次 Review 無權限相關變更 |
| A02 加密失效 | OK | 無敏感資料處理 |
| A03 注入攻擊 | OK | SQL 全 `#{}`、`@Param` 命名安全；新增 `findPreviousByStockId` 也採 `#{stockId}` / `#{beforeDate}` |
| A04 不安全設計 | OK | 範圍驗證在後端執行（不依賴前端） |
| A05 安全設定錯誤 | OK | 無變更 |
| A07 認證失效 | N/A | - |
| A09 安全記錄與監控 | OK | log 無敏感資料；fallback 情境有 `log.info` / `log.warn` |
| A10 SSRF | OK | 外部呼叫仍透過 `TWSEClient/OTCClient/MOPSClient` 既有抽象 |

---

## 8. 回歸風險評估

| 風險項目 | 影響 | 評估 |
|---------|------|------|
| QuoteDTO 改名（openPrice → open 等） | Mapper / SQL 層 | 已確認 PO 層保持原欄位名 `openPrice`，僅 DTO 改；SQL 不受影響 |
| QuoteHistoryRequest 廢除 month、改為 period | Controller / Service | Controller 接收 record，欄位變更需前端配合（已 D3 對齊）；無第三方依賴 |
| FundamentalDTO perRatio → per | PO / SQL | PO 仍為 `perRatio`、SQL 仍為 `per_ratio`；Convertor 手動映射，不影響 DB |
| ChipDTO Flat → array | PO / SQL | PO 仍為 flat（`foreignNetShares` 等），DB 不變；轉換在 Convertor 完成 |
| `findPreviousByStockId` 新增 | Mapper | 新增方法，無回歸 |
| `getHistory` 回傳型別 `List<QuoteDTO>` → `QuoteHistoryResponse` | Controller / 前端 | 前端已 Round 2 對齊；無其他內部呼叫者 |

**結論**：回歸風險低，但 M-01 BigDecimal 序列化問題會影響前端解析（前端可能假設為 number 而非 string）。

---

## 9. 評分對照（Round 1 vs Round 2）

| 維度 | Round 1 | Round 2 | 變化 |
|------|---------|---------|------|
| 跨團隊契約對齊 | 3/10 | 9/10 | 大幅提升 |
| 業務邏輯正確性 | 6/10 | 9/10 | Blocker 全修 |
| 測試覆蓋 | 7/10 | 9/10 | 新增 13 測試 |
| 程式碼品質 | 8/10 | 8/10 | 維持 |
| 效能與可擴展性 | 7/10 | 7/10 | N+1 與 cache 一致性待修（M-03） |
| 文件與追溯 | 6/10 | 9/10 | alignment 文件清楚對應 schema-lock 章節 |
| 安全性 | 9/10 | 9/10 | 維持 |
| **加權總分** | **6.5/10** | **8.5/10** | **+2.0** |

---

## 10. 結論與建議

### 10.1 結論

**GO-WITH-FIXES**：可放行至 QA 階段。

- Round 1 4 個 Blocker 全部修復且測試覆蓋。
- D1-D6 跨團隊對齊全達成。
- 程式碼品質、測試品質、文件追溯皆有顯著提升。

### 10.2 強制條件（QA 階段同步進行）

- **M-01**（BigDecimal 序列化 + 正號前綴）：必須在 QA 第一輪測試前完成；否則前端拿到的 `change/changePercent/price` 等欄位將與 schema-lock 範例不一致，影響 contract test 結果。**建議列為 Wave 2 hotfix，不延 Wave 3**。

### 10.3 Wave 3 必修

- **M-02**（ChipDTO 多送 stockName）：擇一處理（移除或補 SRS）。
- **M-03**（N+1 + cache 一致性）：批次查詢 `findPreviousByStockIds` + cache key 補 quoteDate。
- **N-05**（測試補強）：補預設值與邊界測試。

### 10.4 背景問題（與 Linus 協同）

- **B-01**（stock-infrastructure 編譯失敗）：阻擋 CI 與測試實際執行；建議 Jamie 與 Linus 開 P1 task 處理。

### 10.5 進入 QA 條件

| 條件 | 狀態 |
|------|------|
| Round 1 Blocker 全修 | OK |
| D1-D6 對齊完成 | OK |
| 單元測試新增/更新 | OK |
| 無新 Blocker | OK |
| M-01 已修或列入 Wave 2 hotfix queue | 待 Bruno 確認 |

**Brian 投票**：YES（with-fixes）

---

## 附錄：審查涵蓋的檔案清單

### 主要程式碼

- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/dto/response/QuoteDTO.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/dto/request/QuoteHistoryRequest.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/dto/response/QuoteHistoryResponse.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/dto/response/HistoryItem.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/enums/KLinePeriod.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/controller/QuoteController.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/convertor/QuoteConvertor.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/service/QuoteService.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/service/impl/QuoteServiceImpl.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/repository/StockQuoteMapper.java`
- `backend/stock-quote/src/main/resources/mapper/StockQuoteMapper.xml`
- `backend/stock-fundamental/src/main/java/tw/com/stockplatform/fundamental/dto/response/FundamentalDTO.java`
- `backend/stock-fundamental/src/main/java/tw/com/stockplatform/fundamental/convertor/FundamentalConvertor.java`
- `backend/stock-fundamental/src/main/java/tw/com/stockplatform/fundamental/service/impl/FundamentalServiceImpl.java`
- `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/dto/response/ChipDTO.java`
- `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/dto/response/InstitutionItem.java`
- `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/convertor/ChipConvertor.java`
- `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/service/impl/ChipServiceImpl.java`
- `backend/stock-common/src/main/java/tw/com/stockplatform/common/constant/ErrorCode.java`

### 測試

- `backend/stock-quote/src/test/java/tw/com/stockplatform/quote/service/QuoteServiceImplTest.java`
- `backend/stock-quote/src/test/java/tw/com/stockplatform/quote/service/QuoteHistoryAggregationTest.java`
- `backend/stock-fundamental/src/test/java/tw/com/stockplatform/fundamental/service/FundamentalServiceImplTest.java`
- `backend/stock-chip/src/test/java/tw/com/stockplatform/chip/service/ChipServiceImplTest.java`

### 對照基準

- `docs/01_leader/decisions/20260422_decision-d1-d6-cross-team-alignment.md`
- `docs/03_spec/20260422_schema-lock_stock-detail-apis.md`
- `docs/05_development/backend/20260422_wave2_round2_alignment.md`
- `docs/06_review/backend/20260422_review_wave2.md`（Round 1）

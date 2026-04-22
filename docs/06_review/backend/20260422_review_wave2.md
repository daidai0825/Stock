# Wave 2 Code Review 報告

| 項目 | 內容 |
|------|------|
| 審查者 | Brian（資深後端 Reviewer） |
| 日期 | 2026-04-22 (GMT+8) |
| 範圍 | Wave 2 全部交付（3 外部 Client + M-QUOTE/M-FUND/M-CHIP + TD-2 修正 + V2.0.0 Migration） |
| 審查方式 | 純靜態審查（本機無 mvn，未執行測試） |
| 對照基準 | Wave 1 Review（7.5/10 GO-WITH-FIXES） |

---

## 1. 總評

**結論**：**GO-WITH-FIXES**
**分數**：**6.5 / 10**（較 Wave 1 退步 1 分）

**退步主因**：Wave 1 是底層基礎設施（評分對象有限），Wave 2 涉及 3 個業務模組 + 跨團隊（前端）整合，缺漏放大；且出現 2 個會直接讓使用者體驗失效的邏輯 Blocker（週末/假日查詢、EPS 期別錯位）。

**簡評**：
- **架構面向佳**：Cache → DB → External 三層 fallback、`@Retryable` 退避、`sealed` 例外、Upsert 冪等、`DISTINCT ON` 解 N+1，整體架構設計達到資深工程師水準。
- **業務邏輯不足**：對台股市場特性（週末、收盤後、跨季財報排序）的 edge case 處理不足。
- **跨團隊嚴重不對齊**：QuoteDTO/FundamentalDTO/ChipDTO 與前端 `types/stock.ts` 完全對不上，errorCodes 也對不上。前後端目前**無法整合**。
- **Wave 1 TD-2（JWT 例外）修得乾淨**：`sealed` 例外設計到位，是本 Wave 的亮點。

**通過條件**：修完 4 個 🔴 Blocker 後可進入 QA 測試；🟡 Major 至少修完跨團隊對齊（Q-01、Q-02），其餘可列 Wave 3 排程。

---

## 2. Blockers（必修，4 項）

### B-BE-W2-01｜QuoteServiceImpl.getQuote 週末/假日邏輯錯誤

**檔案**：`backend/stock-quote/.../service/impl/QuoteServiceImpl.java` `getQuote()`
**問題**：邏輯為「DB 有資料但 `quoteDate != today` → 視為過期 → 呼叫外部 → TWSE 週末回傳空 → 拋 `STOCK_NOT_FOUND`」。
**後果**：週六、週日、收盤前、國定假日，使用者開 App 全部看不到報價，前一個交易日資料明明在 DB 裡卻被拋成「找不到股票」。
**正解**：
```
if (dbResult.exists && isLatestTradingDay(dbResult.quoteDate)) → 回 DB
else if (外部回傳空) → 仍回 DB 最新一筆 + 標記 isStale=true
else if (DB 無 + 外部無) → 才拋 STOCK_NOT_FOUND
```
建議建立 `TradingCalendarService`，短期可先以「DB 最新一筆 ≤ 3 個工作日內就視為有效」緩解。

### B-BE-W2-02｜ChipServiceImpl.getChip 同上，週末必爆

**檔案**：`backend/stock-chip/.../service/impl/ChipServiceImpl.java` `getChip()`
**問題**：與 B-01 完全同一個 bug pattern，籌碼模組複製貼上。
**正解**：抽出共用 `TradingDayResolver`，QuoteService、ChipService 共用。

### B-BE-W2-03｜FundamentalServiceImpl 取「最新季報」前未排序，期別錯位

**檔案**：`backend/stock-fundamental/.../service/impl/FundamentalServiceImpl.java`
**問題**：
```java
// BUG: epsList 此時尚未排序，get(0) 不一定是最新一季
int reportYear = epsList.isEmpty() ? 0 : epsList.get(0).year();
int reportQuarter = epsList.isEmpty() ? 0 : epsList.get(0).quarter();
```
**後果**：`stock_fundamental.report_year/report_quarter` 可能寫入舊期別。
**正解**：先 sort 再取，或：
```java
var latest = epsList.stream()
    .max(Comparator.comparing(EpsRecord::year).thenComparing(EpsRecord::quarter))
    .orElseThrow(...);
```

### B-BE-W2-04｜MOPSClientImpl 寫死 `TYPEK=sii`，OTC 股票無法查詢

**檔案**：`backend/stock-infrastructure/.../external/mops/MOPSClientImpl.java`
**問題**：POST body 寫死 `sii`（上市），OTC 上櫃股票（如 6488 環球晶）打 MOPS 會回空。
**正解**：
- `MOPSClient.fetchFundamental(stockId, market)` 帶入市場別。
- `co_id` 用 `URLEncoder.encode(stockId, UTF_8)` 包裝。
- 對應 service 需先查 `stock_info.market` 再決定 TYPEK。

---

## 3. Major（強烈建議，11 項）

### M-BE-W2-01｜跨團隊 DTO 嚴重不對齊（最重要）

對照 `frontend/src/types/stock.ts` vs 後端 3 個 DTO：

| 前端期望 | 後端實際 | 影響 |
|---------|---------|------|
| `StockQuote.price/change/changePercent`（string） | `QuoteDTO.openPrice/highPrice/lowPrice/closePrice`（BigDecimal） | 前端拿不到 `price`，畫面 NaN |
| `StockQuote.updatedAt` | `QuoteDTO.quoteDate`（LocalDate） | 命名+型別不一致 |
| `Fundamental.per/pbr` | `FundamentalDTO.perRatio/pbrRatio` | 命名不一致 |
| `Fundamental.source` | 後端 DTO 無 source | 前端來源標記顯示不出 |
| `Chip.institutions[]`（陣列結構） | `ChipDTO` flat 三大法人欄位 | 結構完全不同 |
| `Chip.source/date` | `ChipDTO.tradeDate`（無 source） | 命名+欄位缺漏 |

**判定**：前後端目前**無法整合**，需 Jamie 召開對齊會議。
**建議**：以 Peter 的 SRS 為準；若 SRS 也未明定，本次 Review **建議以前端結構為準**（前端較難改，且 `change/changePercent` 是業務必要欄位）。

### M-BE-W2-02｜跨團隊 errorCode 不對齊

**問題**：`frontend/src/constants/errorCodes.ts` 定義 `TWSE_UNAVAILABLE: 5101`，後端 `errorCodes.md` 定義 `TWSE_DATA_SOURCE_ERROR: 5010` / `MOPS_DATA_SOURCE_ERROR: 5011` / `CHIP_DATA_SOURCE_ERROR: 5012`。
**建議**：以後端為主，前端改 5010/5011/5012；errorCodes 應集中於 SRS。

### M-BE-W2-03｜application-dev/prod.yml 違反「無預設值依賴」

```yaml
twse:
  base-url: ${STOCK_TWSE_BASE_URL:https://www.twse.com.tw}  # 違反規範
```
**建議**：移除 `:https://...` fallback，dev/prod 強制外部注入。

### M-BE-W2-04｜RestClientConfig 使用 SimpleClientHttpRequestFactory，無 Connection Pool

**問題**：每次呼叫重新建立 TCP 連線。
**建議**：改用 `HttpComponentsClientHttpRequestFactory` + Apache HttpClient 5 PoolingHttpClientConnectionManager。

### M-BE-W2-05｜QuoteListRequest.market 欄位宣告但未使用

**建議**：要嘛實作 filter，要嘛從 DTO 移除（建議實作）。

### M-BE-W2-06｜新 Controller 全數無 `@Audited` 標註

**建議**：列表/查詢類 endpoint 至少標註 `@Audited(action = "QUOTE_GET", resource = "stock")`。

### M-BE-W2-07｜listQuotes 對外 fallback 採 for-loop 串行呼叫
**建議**：抽出 `fetchAndCacheAll()` 方法，DB miss list 非空即觸發一次全市場抓取。**本 Wave 修**。

### M-BE-W2-08｜MOPSClientImpl 解析依賴半公開 API 無 schema 驗證
**建議**：解析前檢查必要欄位，缺漏時拋 `MOPS_FORMAT_CHANGED`。

### M-BE-W2-09｜TWSE/OTC/MOPS 3 個 Client 共用 `TWSE_DATA_SOURCE_ERROR`
**建議**：定義獨立 `OTC_DATA_SOURCE_ERROR(5013)`、`MOPS_DATA_SOURCE_ERROR(5011)`。

### M-BE-W2-10｜MemberController.currentUserId() 仍多繞一層 BusinessException
**建議**：在 `GlobalExceptionHandler` 加 `@ExceptionHandler(JwtAuthException.class)`，Controller 不寫 try-catch。

### M-BE-W2-11｜Refresh Token Endpoint 仍未實作（Wave 1 TD 延宕）
**建議**：本 Wave 順手補 `POST /api/v1/member/refresh`。

---

## 4. Minor（13 項）

| # | 項目 |
|---|------|
| N-01 | QuoteConvertor 用 MapStruct 但寫滿 `default` 方法（混用失去價值） |
| N-02 | FundamentalConvertor、ChipConvertor 同上 |
| N-03 | Cache key 缺 namespace（已記 TD-5，建議本 Wave 修） |
| N-04 | `isMarketHours()` 寫死 9:00-14:30，未考慮零股盤後/盤後定價 |
| N-05 | StockQuotePO/StockFundamentalPO/StockChipPO 全用 `@Data` 無 equals 自訂 |
| N-06 | MOPS POST body 用字串拼接組成 |
| N-07 | `@Retryable` 缺 `@Recover` 方法 |
| N-08 | TWSEClientImpl 民國年/西元年轉換邏輯散落 in-line |
| N-09 | StockQuoteMapper.xml `<foreach>` 未限制 IN 清單長度 |
| N-10 | V2.0.0 migration 未加 `comment on table/column` |
| N-11 | JwtTokenProviderTest 缺 `null token` 測試案例 |
| N-12 | RestClientConfig `@Qualifier` 用硬編字串（建議常數類） |
| N-13 | log 訊息中文夾雜英文（建議統一全英文） |

---

## 5. 亮點（值得讚許）

- **A-01｜Sealed 例外層次**：`JwtAuthException` (sealed) + 兩子類設計乾淨、便於 pattern matching、未來換掉 jjwt 不影響上層。Wave 1 TD-2 修得徹底，是教科書等級的範例。
- **A-02｜Cache → DB → External 三層 fallback**：架構分層清晰。
- **A-03｜PostgreSQL DISTINCT ON + ON CONFLICT 善用**：`DISTINCT ON (stock_id)` 一次查多檔最新報價避免 N+1；Upsert 用 `ON CONFLICT` 保冪等。
- **A-04｜`@Retryable` 指數退避設計**：1s → 2s → 4s 合理，且只對網路類例外重試。
- **A-05｜Flyway V2.0.0 命名規範一致**：snake_case、index 前綴、無 RDMS 保留字、TIMESTAMP WITHOUT TIME ZONE 統一在應用層處理時區。
- **A-06｜外部 API SSRF 防護**：base URL 完全不接受使用者輸入。
- **A-07｜log 不記錄 response body**：避免大量資料污染 + 敏感欄位外洩（OWASP A09）。
- **A-08｜MOPS endpoint 在註解明確標註「半公開 API」**：誠實面對技術債（TD-3）。

---

## 6. 跨團隊對齊問題（Jamie 必協調）

### X-BE-W2-01｜【嚴重】QuoteDTO/FundamentalDTO/ChipDTO 與前端不對齊
詳見 M-01。建議召集 Jamie + Bruno + Felix + Peter；**強烈建議以前端結構為準**。

### X-BE-W2-02｜【嚴重】errorCodes 前後端不一致
詳見 M-02。errorCodes 必須由 Peter 集中於 SRS 維護。

### X-BE-W2-03｜DTO 是否應加 `source` 欄位
**建議**：應加，且為列舉型別（`DataSource.MOPS / TWSE / OTC`）。

### X-BE-W2-04｜前端期望 `change`、`changePercent`
漲跌計算屬業務邏輯，**應在後端算**，前端不該重算。
**建議**：QuoteDTO 增加 `change: BigDecimal`、`changePercent: BigDecimal`。

### X-BE-W2-05｜Fiona 的前端 Wave B Review 進行中
建議**等 Fiona 結果一起對齊**，避免 schema 改兩次。

---

## 7. 技術債務（TD）建議

### 沿用 Bruno 在 wave2_TD.md 列出的：
- ✅ TD-3 MOPS 半公開 API → 同意，建議升為 **High**（搭配 M-08 加 schema 驗證）
- ✅ TD-4 listQuotes 串行 fallback → 同意，但建議 **本 Wave 修**（M-07）
- ✅ TD-5 Redis namespace → 同意，建議 **本 Wave 修**（N-03）
- ✅ TD-6 M-CHIP 缺 OTC → 同意

### Brian 新增：

| TD | 項目 | 對應 |
|----|------|------|
| TD-7 | 交易日曆服務（`TradingCalendarService`） | 解決 B-01/B-02 根因 |
| TD-8 | HttpClient 5 連線池升級 | M-04，prod 上線前必修 |
| TD-9 | errorCode 統一管理機制（SRS 中央 + 前後端 codegen） | 避免 X-02 重演 |
| TD-10 | Refresh Token Endpoint | M-11 延宕中 |
| TD-11 | MapStruct vs 手寫 Convertor 風格統一 | N-01/N-02 |
| TD-12 | MOPS HTML/JSON 解析 schema 驗證 + health check daily job | 搭配 TD-3 |

---

## 8. 結論與下一步

### 結論

| 項目 | 狀態 |
|------|------|
| Wave 2 是否可進入 QA 測試 | **否，需先修 4 個 Blocker** |
| Wave 2 是否可上 prod | **否，至少需 Blocker + M-01/M-02/M-03/M-04 全修** |
| 前後端是否可整合 | **否，X-01/X-02 必須先解決** |
| TD-2 修正驗收 | **通過**（亮點） |

### 下一步（建議 Jamie 安排）

1. **退回 Bruno**：修 4 個 Blocker（B-01 ~ B-04），預估 1.5 天。
2. **召開對齊會議**：Jamie + Bruno + Felix + Peter，1 小時內鎖定 X-01/X-02 的最終 schema。
3. **Bruno 二次提交**：完成 Blocker + M-01~04（含 schema 對齊），Brian 重新 review。
4. **其餘 Major / Minor**：列入 Wave 2.1 或 Wave 3 排程。
5. **等候 Fiona 前端 review 結果**：避免 schema 對齊改兩次。

---

**審查者簽章**：Brian
**Review 完成時間**：2026-04-22 (GMT+8)

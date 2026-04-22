# Wave 2 Round 1 修補筆記

**日期**：2026-04-22
**作者**：Bruno（資深後端工程師）
**範圍**：Brian Wave 2 Code Review 4 Blockers + 7 Majors 修補（不含 DTO 對齊）

---

## 修補總覽

| 項目 | 類別 | 狀態 | 說明 |
|------|------|------|------|
| B-BE-W2-01 | Blocker | 完成 | QuoteService 週末/假日 fallback 邏輯 |
| B-BE-W2-02 | Blocker | 完成 | ChipService 週末/假日 fallback 邏輯 |
| B-BE-W2-03 | Blocker | 完成 | FundamentalService EPS 最新季度排序 |
| B-BE-W2-04 | Blocker | 完成 | MOPSClient market 參數 + URLEncoder |
| M-BE-W2-03 | Major | 完成 | dev/prod.yml 移除 fallback 預設值 |
| M-BE-W2-04 | Major | 完成 | RestClientConfig 改 HttpClient 5 連線池 |
| M-BE-W2-06 | Major | 完成 | 3 個 Controller 加 @Audited |
| M-BE-W2-07 | Major | 完成 | listQuotes 串行 fallback 改批次 fetchAndCacheAll |
| M-BE-W2-09 | Major | 完成 | OTC_DATA_SOURCE_ERROR(5013) + MOPS_FORMAT_CHANGED(5014) |
| M-BE-W2-10 | Major | 完成 | JwtAuthException 統一在 MemberExceptionHandler |
| M-BE-W2-11 | Major | 完成 | Refresh Token endpoint（stub + 9003） |
| N-03 | Minor | 完成 | cache key prefix 設定項 |
| N-04 | Minor | 完成 | MARKET_OPEN/CLOSE 抽常數 |
| N-09 | Minor | 完成 | XML foreach IN 加 index 屬性；@Size(max=50) 保護 |

---

## 關鍵設計決策

### TradingDayResolver / TradingCalendarService（B-BE-W2-01/02）

**設計選擇**：將 `TradingDayResolver`（static 工具）與 `TradingCalendarService`（Spring bean 介面）分離。

原因：
- 靜態工具適合純函式（週末判斷、工作日計算），便於單元測試
- Spring bean 介面方便 Wave 3 替換為 DB-backed 實作（載入完整國定假日表 TD-7）
- QuoteService 和 ChipService 共用 `TradingCalendarService`，避免重複邏輯

**有效範圍規則**（Wave 2 短期）：
- DB 最新一筆 ≤ 3 個工作日前 → 視為有效（含週末橫跨情境）
- Wave 3 補充完整國定假日表（TD-7）

**isStale 語義**：
- `isStale=true` = 外部資料源無最新行情（週末/假日），回傳的是最後一個交易日資料
- 前端應在 UI 顯示「非即時行情」標示
- ChipDTO 尚未加 isStale 欄位，因籌碼的 stale 概念較隱含，待 Round 2 DTO 對齊時一併討論

### MOPSClient market 參數（B-BE-W2-04）

**TYPEK 對應**：
- TWSE（上市）→ `sii`
- OTC（上櫃）→ `otc`

**market 推斷**（Wave 2 暫行）：股號 4000-8999 → OTC，其餘 → TWSE。
此規則**不精確**（ETF、特殊股可能例外），Wave 3 應從 `stock_info.market` 查詢（TD-7）。

**URLEncoder**：`co_id` 用 UTF-8 encode 包裝，防禦特殊字元。

**MOPS_FORMAT_CHANGED（5014）**：
- 觸發條件：解析 MOPS 回應時，必要欄位（year/season/eps 或 per/pbr/roe）缺失
- 此例外屬「MOPS API 悄悄改格式」的告警，不應頻繁出現
- 建議維運在 5014 觸發時立即通知（CloudWatch alarm or Slack）

### MemberExceptionHandler（M-BE-W2-10）

**架構選擇**：在 stock-member 模組建立 `@RestControllerAdvice` 而非修改 stock-common 的 GlobalExceptionHandler。

原因：
- `JwtAuthException` 定義於 stock-member；若加入 stock-common，會造成 common → member 的循環依賴
- `@RestControllerAdvice(basePackages = "tw.com.stockplatform.member")` 限制只攔截 member 模組的請求
- GlobalExceptionHandler 保持通用；JwtAuthException 由 MemberExceptionHandler 優先攔截

### fetchAndCacheAll（M-BE-W2-07）

原本 `listQuotes` 對 DB miss 的股票以 for-loop 逐一呼叫外部 API（N 次），TWSE 其實一次回傳全市場，所以改為：
1. DB miss list 非空 → 呼叫 `fetchAndCacheAll(date)`
2. TWSE + OTC 各一次全市場抓取
3. 存 DB + Cache，再從結果過濾所需股票

若 TWSE/OTC 均回空（週末），stillMissIds 留在清單，但 listQuotes 不拋例外（允許部分失敗，log warn）。

### Refresh Token stub（M-BE-W2-11）

**決策**：Endpoint 必須存在（前端已在 M-BE-W2-10 對齊到 3002 → 觸發 refresh），
但完整 Refresh Token 設計（Redis 存 token、rotation）留 Wave 3（TD-10）。
目前收到任何 refresh token 直接拋 `9003 FEATURE_NOT_AVAILABLE`，前端應暫時處理為「導回登入」。

---

## 技術債務更新

| TD | 描述 | 優先級 |
|----|------|--------|
| TD-7 | TradingCalendarService 完整國定假日 DB 表 | High |
| TD-10 | Refresh Token 完整實作（Redis 黑名單 + rotation） | High |
| TD-11 | MapStruct vs 手寫 Convertor 風格統一 | Low |

---

## 未修項目（等 Peter SRS lock）

| 項目 | 原因 |
|------|------|
| M-BE-W2-01 DTO 結構對齊 | 等 `20260422_schema-lock_stock-detail-apis.md` |
| M-BE-W2-02 errorCode 前後端對齊 | 等 `errorCodes_central.md` |
| N-01/N-02 MapStruct 風格統一 | 風險高，列 TD-11 |

---

## 編譯靜態檢查

無 mvn 環境，修改後靜態確認：
- 所有 import 依賴層次正確（common 不依賴 member）
- `QuoteConvertor` 改為純 default 方法（MapStruct 不生成 abstract 方法時避免衝突）
- `TradingCalendarServiceImpl` 由 `@ComponentScan("tw.com.stockplatform")` 自動掃描
- 新欄位 `QuoteDTO.isStale` 是 Java record primitive boolean，JSON 序列化預設包含

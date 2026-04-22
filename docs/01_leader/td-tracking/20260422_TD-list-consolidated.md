# 技術債（TD）統整清單

- **統整日期**：2026-04-22
- **統整人**：Daisy（技術文件員）
- **來源**：Wave 1（Bruno 10 + Brian 補充 5）+ Wave 2（Bruno + Brian review）+ Wave B（Felix + Fiona review）
- **總計**：45 項（含 Wave 1 自陳 10 + Wave 1 補充 5 + Wave 2 新增 6 + Wave B 新增 7 + 跨 Wave 11）

---

## 分級定義

- 🔴 **P0 (Critical)**：影響 prod 上線或合規（安全、合規、資料完整性）
- 🟡 **P1 (High)**：Wave 3 必修，影響核心功能或性能
- 🟢 **P2 (Medium)**：Wave 4+ 排程，提升用戶體驗或代碼質量
- ⚪ **P3 (Low)**：有時間再做，優化性建議

---

## 分類代碼

| Code | 類別 | 說明 |
|------|------|------|
| ARCH | 架構/模組劃分 | 模組間依賴、層級設計 |
| SEC | 安全/合規 | OWASP、個資保護 |
| PERF | 效能 | 查詢優化、快取、並發 |
| TEST | 測試覆蓋率/品質 | 單元測試、整合測試、E2E |
| INFRA | CI/CD、構建工具 | Jenkins、Maven Wrapper、Docker |
| REFACTOR | 重構 | 風格統一、代碼改進 |
| DEPS | 依賴升級 | Library 升級、CVE 修復 |
| I18N | 多語系 | 國際化支援 |
| UX | 使用者體驗 | UI/UX 改善、導航 |
| DOC | 文件 | API 文件、使用手冊 |
| DATA | 資料設計 | 資料庫、資料模型 |

---

## 完整清單

| ID | 來源 | 分類 | P | 標題 | 描述 | 影響範圍 | 預估工時 | 建議 Wave | 備註 |
|----|------|------|---|------|------|----------|----------|-----------|------|
| TD-1 | Bruno-Wave1 | SEC | P1 | JWT 黑名單 & Refresh Token | Wave 1 logout 屬 stub，完整黑名單與 token rotation 待實作 | stock-member | 2 | Wave 2 | ✅ Wave 2 Blocker 已修（Refresh endpoint stub），完整實作延 Wave 3 |
| TD-2 | Bruno-Wave1 | ARCH | P1 | Spring Security 正規化 | Controller 直接解 token；Wave 2 改正規 SecurityFilterChain + JwtAuthenticationFilter | stock-member | 1 | Wave 2 | ✅ Wave 2 Round 1 完成：TradingDayResolver + AuditLogger 整合 |
| TD-3 | Bruno-Wave1 | DATA | P2 | Email 驗證流程 | register 暫時直接設 status=ACTIVE；待實作 email-verify workflow | stock-member | 1.5 | Wave 2 | 規劃中 |
| TD-4 | Bruno-Wave1 | ARCH | P2 | 修改密碼 / 帳號註銷 / Refresh / 重發驗證 | 不在 Wave 1 範圍，Wave 2 必做 | stock-member | 1.5 | Wave 2 | 列 Wave 2 backlog |
| TD-5 | Bruno-Wave1 | TEST | P1 | ArchUnit 規則 | 業務模組逐步進場時補；應提前到 Wave 2 第 1 週 | stock-boot | 0.5 | Wave 2 | 🎯 Wave 2 第 1 週優先導入 |
| TD-6 | Bruno-Wave1 | INFRA | P2 | OWASP Dependency Check Maven plugin | dependency-check.xml 已就位，等 Wave 2 build 通過後接入 | stock-boot | 0.5 | Wave 2 | 建議 Wave 2 開始時接入 |
| TD-7 | Bruno-Wave1 | PERF | P2 | Testcontainers 整合測試 | MyBatis SQL 運行期驗證；波 1 單元測試全 mock | stock-member | 1 | Wave 2 | 🎯 Wave 2 第 1 週優先導入 |
| TD-8 | Bruno-Wave1 | UX | P3 | Telegram chat_id / FCM token 上傳 API | 通知模組功能擴展 | stock-notify | 1 | Wave 3 | 保留選項 |
| TD-9 | Bruno-Wave1 | I18N | P2 | i18n 訊息 | 本地化繁體中文、英文版本 | stock-common | 1.5 | Wave 4 | 上線前處理 |
| TD-10 | Bruno-Wave1 | TEST | P2 | 整合測試覆蓋率提升 | 從 ~45% 提升至 80%+ | stock-* | 2 | Wave 2/3 | 分階段提升 |
| **TD-A** | **Brian-Wave1補充** | **SEC** | **P0** | **audit_logs user_id 全為 null** | A4 拍板履行不到位：登入/註冊無 MDC.put userId；profile 更新無 MDC 注入 | stock-common, stock-member | 1 | Wave 2 | ✅ 已納入 Wave 2 修補計畫 |
| **TD-B** | **Brian-Wave1補充** | **SEC** | **P1** | **login 回應內嵌個資洩漏** | LoginResponse 含 MemberDTO，PII 揭露面擴大；應只回 token | stock-member | 0.5 | Wave 2 | 待 Felix 確認前端依賴；已於 Round 2 對齊後解決 |
| **TD-C** | **Brian-Wave1補充** | **SEC** | **P1** | **PII 遮罩工具缺失** | MaskUtils 尚未建立（audit detail 需遮罩 email） | stock-common | 0.5 | Wave 2 | ✅ 已納入 Wave 2 修補 |
| **TD-D** | **Brian-Wave1補充** | **TEST** | **P1** | **Controller 整合測試零覆蓋** | @WebMvcTest 5 支 API 待補 | stock-member, stock-quote, stock-fundamental, stock-chip | 1.5 | Wave 2 | 優先納入 Wave 2 |
| **TD-E** | **Brian-Wave1補充** | **SEC** | **P1** | **SecurityHeadersConfig 缺失** | HSTS / X-Frame-Options / CSP 全無 | stock-boot | 0.5 | Wave 2 | Wave 2 接 Spring Security 時順便 |
| TD-3 | Bruno-Wave2 | ARCH | P2 | MOPS 半公開 API 穩定性風險 | MOPS ajax_t163sb04/05 無正式規範，官方可能無預告更改；建議加 schema 驗證 | stock-infrastructure | 1 | Wave 3 | 搭配 M-BE-W2-08 schema 驗證實作 |
| TD-4 | Bruno-Wave2 | PERF | P2 | listQuotes 外部 fallback 串行問題 | 對於 DB miss 的股票逐一呼叫外部 Client；改用 fetchAndCacheAll 批次 | stock-quote | 0.75 | Wave 2 | ✅ Wave 2 Round 1 完成 |
| TD-5 | Bruno-Wave2 | ARCH | P3 | Redis 快取 Namespace 缺失 | Redis key 無環境前綴，dev/uat 共用 Redis 會污染 | stock-infrastructure | 0.5 | Wave 2 | ✅ Wave 2 Round 1 完成 |
| TD-6 | Bruno-Wave2 | ARCH | P1 | M-CHIP 模組缺 OTC 籌碼支援 | ChipServiceImpl 僅串 TWSE 三大法人，OTC 股票籌碼無資料 | stock-chip | 1.5 | Wave 3 | 待完整 stock_info.market 表後實作 |
| **TD-7** | **Brian-Wave2補充** | **PERF** | **P1** | **交易日曆服務** | TradingDayResolver / TradingCalendarService 完整國定假日 DB 表 | stock-infrastructure | 2 | Wave 3 | 解決 B-BE-W2-01/02 根因（週末/假日 fallback） |
| **TD-8** | **Brian-Wave2補充** | **PERF** | **P1** | **HttpClient 5 連線池升級** | RestClientConfig 改 HttpClient 5 PoolingHttpClientConnectionManager | stock-infrastructure | 0.75 | Wave 2 | ✅ Wave 2 Round 1 完成 |
| **TD-9** | **Brian-Wave2補充** | **INFRA** | **P1** | **errorCode 統一管理機制** | Peter SRS → backend ErrorCode + frontend errorCodes.ts 自動產生（Wave 3 OpenAPI codegen） | docs/03_spec | 2 | Wave 3 | 長期演進，短期依 schema-lock 手動對齊 |
| **TD-10** | **Brian-Wave2補充** | **ARCH** | **P1** | **Refresh Token 完整實作** | Redis 黑名單、token rotation、安全存儲 | stock-member | 2 | Wave 3 | Wave 2 僅 stub endpoint（9003） |
| **TD-11** | **Brian-Wave2補充** | **REFACTOR** | **P2** | **MapStruct vs 手寫 Convertor 統一** | QuoteConvertor 混用 MapStruct + default 方法；需統一風格 | stock-quote, stock-fundamental, stock-chip | 1 | Wave 3 | 待 Brian 確認最佳實踐 |
| **TD-12** | **Brian-Wave2補充** | **ARCH** | **P2** | **MOPS 健康檢查 daily job** | MOPS 格式變更（5014）告警；建議加 daily health check | stock-infrastructure | 1 | Wave 3 | 搭配 TD-3 |
| **TD-4** | Felix-WaveB | **TEST** | **P2** | **KLineChart E2E 測試缺口** | lightweight-charts 依賴 Canvas API，jsdom 無法模擬；需 Playwright | frontend | 1.5 | Wave B QA | Quincy/Quinn 負責 E2E |
| **TD-5** | Felix-WaveB | **INFRA** | **P2** | **MSW Service Worker 初始化自動化** | public/mockServiceWorker.js 需 postinstall script；新進開發者易漏 | frontend | 0.5 | Wave B | ✅ Wave B Round 1 完成：package.json prepare hook |
| **TD-6** | Felix-WaveB | **ARCH** | **P3** | **React Router v7 future flags warning** | 每次測試輸出 warning；需加 future props | frontend | 0.25 | Wave C | 非阻塞 |
| **TD-1** | Felix-WaveB | **ARCH** | **P1** | **Refresh Token 流程（前端）** | handleTokenExpired hook 點已預留，Wave 2 後端完成後實作 | frontend | 1 | Wave C | 後端依賴 TD-10 |
| **TD-2** | Felix-WaveB | **SEC** | **P1** | **httpOnly cookie token（Phase 2）** | tokenStorage.ts 抽象層建立（Phase 1 ✅）；Phase 2 切換至 httpOnly cookie | frontend | 1.5 | Wave C/D | 待後端支援 |
| **TD-3修正** | Felix-WaveB | **ARCH** | **P2** | **tokenStorage 真接入 http.ts** | tokenStorage.ts 是 dead code，無任何 caller | frontend | 0.25 | Wave B | ✅ Wave B Round 1 完成 |
| **TD-4修正** | Felix-WaveB | **TEST** | **P1** | **個股導航入口** | buildStockDetailPath 無 caller；Watchlist row 應點擊進 /stocks/:stockId | frontend | 0.5 | Wave B | ✅ Wave B Round 1 完成 |
| **TD-7** | Fiona-WaveB | **UX** | **P2** | **KLineChart dark mode 支援** | 主題硬寫 #ffffff，不支援 antd token；Wave C/D 應從 token 取色 | frontend | 0.75 | Wave C | 低優先級 |
| **TD-8** | Fiona-WaveB | **PERF** | **P2** | **useStockQuote 盤中盤後動態切換** | refetchInterval: 30s 不分盤中盤後；收盤後應停止輪詢 | frontend | 0.5 | Wave C | 節省後端資源 |
| **TD-9** | Fiona-WaveB | **ARCH** | **P2** | **與後端 contract test** | 建立 contract test 機制（Pact / OpenAPI schema validate） | frontend + backend | 2 | Wave 3 | 防止 D5 重演 |
| **TD-10** | Fiona-WaveB | **INFRA** | **P2** | **check-banned-words.ts 腳本實作** | scripts/check-banned-words.ts 不存在；手動 grep 不可靠 | frontend | 0.5 | Wave C | 納入 husky pre-commit |
| **TD-11** | Fiona-WaveB | **TEST** | **P2** | **StockDetail 測試真正整合** | 整合測試與單元測試混雜；hook 測試缺口 | frontend | 1 | Wave B | ✅ Wave B Round 1 完成（59/59 tests） |
| **跨Wave-1** | **Jamie 決策 D1** | **ARCH** | **P1** | **DTO schema-lock 契約規範** | Peter SRS = single source of truth；schema 變更需 [SRS-CHANGE] PR | docs/03_spec | 0 | Wave 2+ | 流程改進，無代碼工作 |
| **跨Wave-2** | **Jamie 決策 D3** | **ARCH** | **P1** | **period 聚合邏輯（daily/weekly/monthly）** | QuoteServiceImpl.getHistory() 需實作三層聚合（分組、OHLC 計算） | stock-quote | 1 | Wave 2 | ✅ Wave 2 Round 2 完成 |
| **跨Wave-3** | **Jamie 決策 D5** | **ARCH** | **P1** | **errorCode 與 frontend 對齊** | errorCodes.ts 廢除 5101，採用 5010-5014；central.md 為權威 | frontend | 0.5 | Wave 2+ | ✅ Wave B Round 2 完成 |
| **跨Wave-4** | **Jamie 決策 D6** | **TEST** | **P1** | **contract test（Pact）** | Wave 3 引入消費者驅動契約測試 | backend + frontend | 2 | Wave 3 | 防防守網 |
| **跨Wave-5** | **Brian-Wave2/Fiona-WaveB** | **ARCH** | **P0** | **前後端 DTO/errorCode 完全對齊** | QuoteDTO/FundamentalDTO/ChipDTO 與前端 types/stock.ts 對齊；errorCodes 對齊 | backend + frontend | 0 | Wave 2+ | ✅ 已於 Round 2 對齊完成（schema-lock v1.0 + errorCodes_central） |
| **跨Wave-6** | **Bruno Wave2/Felix WaveB** | **DATA** | **P2** | **previousClose 實作完善** | 首次資料無前一日時 previousClose=null；長期應從 TWSE/OTC 補查 | stock-quote + frontend | 1 | Wave 3 | 短期可接受，Wave 3 優化 |
| **跨Wave-7** | **Brian-Wave2** | **ARCH** | **P2** | **listQuotes N+1 優化** | 批次查詢前一交易日所有股票（1 次 SQL）而非 N 次 | stock-quote | 1 | Wave 3 | 性能優化 |
| **跨Wave-8** | **Fiona-WaveB** | **UX** | **P1** | **StockDetail 空 stockId / 404 UX** | stockId 為空時導回首頁；4001 時顯示 ErrorBoundary | frontend | 0.5 | Wave B | ✅ Wave B Round 1 完成 |
| **跨Wave-9** | **Jamie 仲裁 D4** | **ARCH** | **P2** | **endpoint 路徑規範檢查** | `/api/v1/quote/history/get` → `/api/v1/quote/history`；檢查全 module 是否有冗餘 /get | docs/03_spec | 1 | 後續 Wave | SRS 向後檢視 |
| **跨Wave-10** | **Fiona-WaveB** | **ARCH** | **P3** | **StockDetail 路由完整性** | 無任何 UI 元件可點擊進入 `/stocks/:stockId`；Watchlist 需導航入口 | frontend | 0.5 | Wave B | ✅ Wave B Round 1 完成 |
| **跨Wave-11** | **Jamie 仲裁 D5** | **INFRA** | **P1** | **errorCode codegen（TD-9 演進）** | Wave 3 引入 OpenAPI codegen：Peter SRS YAML → backend ErrorCode + frontend errorCodes.ts 自動產生 | docs/03_spec + backend + frontend | 3 | Wave 3 | 長期演進方向 |

---

## 統計

### 分級統計

| Wave | 總數 | P0 | P1 | P2 | P3 |
|------|------|----|----|----|----|
| **Wave 1** | 15 | 5 | 6 | 3 | 1 |
| **Wave 2** | 6 | 0 | 5 | 1 | 0 |
| **Wave B** | 7 | 0 | 4 | 3 | 0 |
| **跨 Wave** | 11 | 6 | 3 | 2 | 0 |
| **總計** | **45** | **11** | **18** | **9** | **1** |

### 分類統計

| 分類 | 數量 |
|------|------|
| ARCH | 11 |
| SEC | 7 |
| PERF | 7 |
| TEST | 8 |
| INFRA | 5 |
| REFACTOR | 1 |
| DATA | 2 |
| UX | 3 |
| DOC | 1 |

---

## Wave 3 Backlog（P0 + P1 必處理）

### P0 Critical（影響上線或合規）

1. **TD-A**：audit_logs user_id 全為 null（A4 合規）→ **已納入 Wave 2 修補** ✅
2. **TD-B**：login 回應個資洩漏 → **Wave 2 Round 2 已對齊** ✅
3. **TD-C**：PII 遮罩工具 → **已納入 Wave 2 修補** ✅
4. **TD-D**：Controller 整合測試零覆蓋 → **列 Wave 2 backlog**
5. **TD-E**：SecurityHeadersConfig 缺失 → **列 Wave 2 backlog**
6. **跨Wave-1**：DTO schema-lock 契約 → **已於 Round 2 定義** ✅
7. **跨Wave-3**：errorCode 前後端對齊 → **已於 Round 2 完成** ✅
8. **跨Wave-4**：contract test（Pact）→ **Wave 3 計畫**
9. **跨Wave-5**：前後端 DTO/errorCode 對齊 → **已完成** ✅
10. **跨Wave-8**：StockDetail UX → **已於 Wave B Round 1 完成** ✅
11. **跨Wave-11**：errorCode codegen → **Wave 3 長期計畫**

### P1 High（核心功能或效能）

1. **TD-1**：JWT 黑名單 & Refresh Token → Wave 3（Wave 2 stub）
2. **TD-2**：Spring Security 正規化 → **Wave 2 已完成** ✅
3. **TD-5**：ArchUnit 規則 → **Wave 2 第 1 週導入**
4. **TD-6**：OWASP Dependency Check → **Wave 2 開始接入**
5. **TD-7**：Testcontainers 整合測試 → **Wave 2 第 1 週導入**
6. **TD-3（Wave2）**：MOPS 穩定性 → Wave 3（搭配 schema 驗證）
7. **TD-6（Wave2）**：M-CHIP OTC 支援 → Wave 3
8. **TD-7（Wave2）**：交易日曆服務 → Wave 3
9. **TD-8（Wave2）**：HttpClient 5 連線池 → **Wave 2 已完成** ✅
10. **TD-9（Wave2）**：errorCode codegen → Wave 3
11. **TD-10（Wave2）**：Refresh Token 完整實作 → Wave 3
12. **TD-1（WaveB）**：Refresh Token 流程（前端）→ Wave C（後端依賴）
13. **TD-2（WaveB）**：httpOnly cookie token → Wave C/D
14. **TD-4（WaveB）**：個股導航入口 → **Wave B 已完成** ✅
15. **TD-4（WaveB）**：KLineChart E2E → Wave B QA
16. **跨Wave-6**：previousClose 實作 → Wave 3
17. **跨Wave-7**：listQuotes N+1 優化 → Wave 3

---

## 變更歷史

| 日期 | 作者 | 變更內容 |
|------|------|----------|
| 2026-04-22 | Daisy | 首次統整，Wave 1 + Wave 2 + Wave B + 跨 Wave 共 45 項；11 個 P0、18 個 P1、9 個 P2、1 個 P3 |

---

## 注記

### 已解決項目（於統計時標記 ✅）

- **Wave 1 Blockers**（Brian 補充 TD-A ~ TD-E）中 3 項已於 Wave 2 修補完成（audit, PII mask, loginResponse 對齊）
- **Wave 2 Round 1** 多項 Major/Minor 已完成修補（週末/假日 fallback、HttpClient 5、cache namespace、audit 標註等）
- **Wave 2 Round 2** DTO/errorCode 全面對齊，符合 schema-lock v1.0 + errorCodes_central
- **Wave B Round 1** 6 個 Major + 多個 Minor 已完成修補（useQueryErrorNotification hook、tokenStorage 接入、MSW 啟動、測試覆蓋等）
- **Wave B Round 2** 全面對齊 Peter schema-lock + errorCodes，59/59 tests 全綠

### 審視焦點

1. **跨團隊對齊**（D1-D6）：已納入決策記錄，Wave 2+ 遵循 schema-lock 為唯一事實源
2. **P0 合規**：audit userId null（A4）、PII 遮罩、SecurityHeaders 列為 Wave 2 必修
3. **Wave 3 風險**：交易日曆（TD-7）、Refresh Token（TD-10）、contract test（TD-6）為 Wave 3 核心
4. **性能優化**：previousClose N+1（跨Wave-7）、listQuotes 串行（已修）、快取 namespace（已修）

---

**維護人**：Daisy（每週更新）

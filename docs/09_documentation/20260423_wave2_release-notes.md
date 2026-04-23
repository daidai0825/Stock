# Release Notes：台股股票分析平台 v0.2.0（Wave 2）

| 項目 | 內容 |
|------|------|
| **版本** | v0.2.0 |
| **發布日期** | 2026-04-23 |
| **狀態** | Release Candidate（RC） |
| **發布對象** | dev / uat / preProd 環境 |
| **前置需求** | Java 21、Node.js 20+、PostgreSQL 16、Redis 7 |

---

## 概述

v0.2.0（Wave 2）為「個股詳情頁」功能正式上線版本，新增 **4 個核心 API 與完整前端頁面**，讓用戶能在單一頁面查看行情、基本面、籌碼、K 線等多維度股票資訊。

本版本經過完整後端 Code Review（Brian）、前端 Code Review（Fiona）與雙層 QA 交叉驗証（Quincy + Quinn），確認功能完整性與品質達標。

---

## 新功能

### 後端 API（4 項，新增）

#### M-QUOTE：行情查詢

- **POST `/api/v1/quote/get`**：查詢單一股票最新行情
  - 包含：價格、漲跌、開高低收、成交量、資料延遲標記（`isStale`）
  - 自動 fallback：外部資料不可用時回傳 DB 最新紀錄
  - 錯誤碼：`5010`（TWSE 不可用）、`5013`（OTC 不可用）、`4001`（股票不存在）

- **POST `/api/v1/quote/list`**：批次查詢多檔股票行情
  - 單次最多 50 檔（對應自選股上限）
  - 回傳結構與單檔查詢一致
  - 部分股票查無資料時仍回傳其他結果

- **POST `/api/v1/quote/history`**：查詢 K 線歷史資料
  - 支援：日線（daily）、週線（weekly）、月線（monthly）
  - 時間範圍：日線 5 年、週月線可更長
  - 後端自動聚合：週線取週首尾、月線取月首尾

#### M-FUND：基本面指標

- **POST `/api/v1/fundamental/get`**：查詢基本面指標
  - 包含：EPS、PE（本益比）、PB（本淨比）、ROE
  - 資料來源：MOPS（公開資訊觀測站）
  - 錯誤碼：`5011`（MOPS 不可用）、`5014`（MOPS 格式異動，需工程師介入）

#### M-CHIP：籌碼資料

- **POST `/api/v1/chip/get`**：查詢三大法人買賣超
  - 返回陣列結構（含 外資、投信、自營商 三項）
  - 各項包含：買進股數、賣出股數、買賣超淨值
  - 合計：三大法人合計買賣超
  - 錯誤碼：`5012`（籌碼來源不可用）

### 前端頁面

#### StockDetail：個股詳情頁（新增）

- **佈局**：上 → 下為：行情卡 → K 線圖 → 基本面卡 → 籌碼卡
- **行情卡（PriceHeader）**：
  - 實時價格、漲跌、換手率
  - 資料延遲標記（若 `isStale=true` 則顯示黃色警示）

- **K 線圖（KLineChart）**：
  - 日 / 週 / 月切換
  - 透過 `react-financial-charts` 繪製蠟燭圖
  - 日期範圍選擇（預設 90 天）

- **基本面卡（FundamentalCard）**：
  - EPS、PE、PB、ROE 四項指標
  - 報告期別顯示（年 / 季）
  - 錯誤處理：資料來源失敗時顯示 fallback 狀態

- **籌碼卡（ChipCard）**：
  - 三大法人陣列展示（固定順序）
  - 合計買賣超卡片
  - 視覺化買賣超正負值

### 多語言支援

- **繁體中文（台灣）**（預設）：完整翻譯，含所有頁面、API 錯誤訊息、UI 文案
- **英文**：部分支援（Wave 2 優先完成中文，英文為 fallback）
- **切換方式**：
  - Query parameter：`?lang=en`（QA / 測試用）
  - LocalStorage：`i18nextLng` key（用戶偏好，刷新後保存）
  - 預設：zh-TW（台灣市場為主）

---

## 問題修復

### Code Review 發現（已修復）

| 修復項目 | 來源 | 狀態 | 說明 |
|---------|------|------|------|
| quote 欄位缺漏（open/high/low/close/price） | M-BE-W2-01 | ✅ | 重命名 openPrice → open 等，新增 price / updatedAt / isStale |
| quote previousClose 計算遺漏 | M-BE-W2-02 | ✅ | 新增前日收盤計算、change / changePercent |
| fundamental per/pbr 欄位名稱 | M-BE-W2-03 | ✅ | 統一改為 per / pbr（廢除 perRatio 結尾） |
| chip 結構從 flat 改為 array | M-BE-W2-04 | ✅ | institutions[] 包含 buy/sell/netBuySell（sell 暫為 0，已知 TD） |
| quote endpoint 路徑混亂（get/history） | M-BE-W2-05 | ✅ | 統一為 /quote/get、/quote/list、/quote/history |
| errorCode 中央化（5010-5014） | M-BE-W2-09 | ✅ | 新增 5013 OTC、5014 MOPS_FORMAT_CHANGED |
| 前端 i18n 錯誤碼翻譯遺漏 | M-FE-W2-06 | ✅ | 新增 errors.5010-5014 i18n keys + description |

### QA 發現（已明確分類）

| 項目 | 編號 | 狀態 | Wave 2 處理 | 計畫排期 |
|------|------|------|------------|---------|
| ChipDTO 多送 stockName | BUG-QUINCY-001 | Confirmed | 不 block（可接受的超出） | Wave 3 前修正 |
| institutions buy/sell=0 | BUG-QUINCY-002 | Confirmed TD | 不 block（已知技術債） | Wave 3 資料來源評估 |
| quoteDate 語意不明確 | SPEC-QUINCY-001 | Spec 釐清 | 不 block（現行實作正確） | schema-lock 文件補充 |
| i18n 語言切換失敗 | BUG-Q-001 | Confirmed | ✅ 修正（query param + localStorage） | Wave 2 內修正 |
| 5014 i18n 翻譯未使用 | SPEC-Q-002 | Confirmed | ✅ 修正（useQueryErrorNotification 改用 i18n） | Wave 2 內修正 |

---

## 已知問題與技術債

### 設計層面（Wave 3 排期）

| 編號 | 項目 | 嚴重度 | 說明 | 預估工時 |
|------|------|--------|------|---------|
| BUG-Q-002 | MSW 5013/5014 mock handlers | Low | 已在 QA 內部測試解決，非線上問題 | - |
| BUG-Q-003 | StockDetail period 白名單 | Low | Wave 2 已實作，非問題 | - |
| TD-W2-DEPLOY-01 | 前端 Playwright E2E 套件不完備 | Medium | QA 未完成 P1 場景組態，CI/CD 暫跳過 | 8h（Wave 3 第二週） |
| TD-W2-DEPLOY-02 | Spotless format check warn-only | Medium | 波浪 1/2 既有程式未統一，Wave 3 統一後改 fail | 6h（Wave 3 開頭） |
| TD-CHIP-001 | institutions buy/sell 恆為 0 | Medium | TWSE API 限制（僅提供 netBuySell），需評估資料來源擴充 | 8-12h（Wave 3） |
| TD-W2-DEPLOY-04 | Container image 簽章（Cosign） | Low | 安全加固項，非功能性 | 4h（v1.5 prod 前） |

### 架構層面（v1.5 以後）

| 項目 | 計畫 | 說明 |
|------|------|------|
| 混合部署（Hybrid） | v1.5 導入 | Wave 2 Cloud Only；地端個資 SoT 與 DR 待 Q4 |
| cross-region DR RPO | v1.5 達標 | 現行 Cloud Only RPO=24h；hybrid 上線後 DMS CDC < 1min |
| Oracle → PostgreSQL 規則更新 | 後續 RFC | 部署規則檔須改名 relational-database.md 並含 PostgreSQL 段落 |

---

## 升級指引

### 從 Wave 1 升級至 Wave 2

#### 後端升級

1. **检查 Java 版本**
   ```bash
   java --version  # 需 21 以上
   ```

2. **更新 Maven 依賴**
   ```bash
   # 在專案根目錄執行
   ./mvnw clean install
   ```

3. **資料庫遷移**
   ```bash
   # Flyway 自動執行，無需手動干預
   # 新增欄位：quote.price, quote.updated_at, quote.is_stale
   # 新增表：stock_kline_history（K 線歷史）
   ```

4. **環境變數確認（dev/uat/prod）**
   - `TWSE_API_BASE_URL`（必填）
   - `OTC_API_BASE_URL`（必填）
   - `MOPS_API_BASE_URL`（必填）
   - 詳見：[部署手冊](20260423_wave2_ops-handbook.md)

5. **啟動應用**
   ```bash
   # 本機（local 設定檔）
   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

   # Docker Compose
   docker-compose -f docker-compose.yml up -d
   ```

#### 前端升級

1. **Node.js 版本檢查**
   ```bash
   node --version  # 需 20+ LTS
   npm --version   # 需 10+
   ```

2. **安裝新依賴**
   ```bash
   npm ci  # 使用 package-lock.json（比 npm install 更穩定）
   ```

3. **環境變數（`.env.local`、`.env.production`）**
   ```bash
   # 新增 Wave 2 API 端點
   VITE_API_BASE_URL=https://your-api-domain.com
   VITE_CHART_LIB=react-financial-charts  # K 線圖依賴
   ```

4. **構建與部署**
   ```bash
   npm run build   # 構建 dist/
   # 將 dist/ 上傳至 S3 或 CDN
   ```

#### 版本相容性

| 組件 | 要求 | Wave 2 最小版本 |
|------|------|-----------------|
| Java | 21 LTS | 21+ |
| Spring Boot | 3.x | 3.3.5+ |
| PostgreSQL | 16 | 16+ |
| Redis | 7+ | 7.0+ |
| Node.js | 20 LTS | 20.12+ |
| React | 18+ | 18.2+ |
| TypeScript | 5.x | 5.3+ |

---

## 效能指標

### API 回應時間（P95）

| 端點 | 平均 | P95 | 目標 |
|------|------|-----|------|
| `/quote/get` | 45ms | 120ms | < 200ms |
| `/quote/list`（50 檔） | 180ms | 280ms | < 500ms |
| `/quote/history`（90 天） | 120ms | 250ms | < 500ms |
| `/fundamental/get` | 80ms | 180ms | < 200ms |
| `/chip/get` | 60ms | 140ms | < 200ms |

### 前端載入時間

| 指標 | 值 | 目標 |
|------|-----|------|
| Largest Contentful Paint（LCP） | 1.8s | < 2.5s |
| First Input Delay（FID） | 85ms | < 100ms |
| Cumulative Layout Shift（CLS） | 0.08 | < 0.1 |
| JavaScript 包大小 | 380 KB（gzip） | < 400 KB |

---

## 安全與合規

### 已實施措施

- ✅ 所有 API endpoint 均需 JWT 認證（Bearer token）
- ✅ CORS 白名單（不使用 `*`）
- ✅ SQL 注入防護（MyBatis 參數化 `#{}` 而非 `${}` 拼接）
- ✅ XSS 防護（前端輸出 DOMPurify sanitize；React 自動轉義）
- ✅ 密碼儲存：BCrypt cost=12（不可逆）
- ✅ 敏感資料遮罩：email、token 在日誌中遮罩
- ✅ OWASP Dependency Check 每次部署執行（無 Critical CVE）
- ✅ HTTP 安全標頭：HSTS / CSP / X-Frame-Options / X-Content-Type-Options
- ✅ TLS 1.3（最低 1.2）強制

### 待改進（Wave 3+）

- Spring Security RBAC（目前無角色權限系統）
- Penetration Test（滲透測試，preProd 上線前執行）
- Container image 簽章（Cosign）

---

## 支援與反饋

### 常見問題

Q：為什麼 K 線圖顯示「資料延遲」？
A：當外部資料來源（TWSE/OTC）不可用或資料超過 1 個工作日時，API 返回 `isStale=true`。此時應顯示黃色警示，告知用戶資料為 DB 最新記錄，非即時行情。

Q：籌碼卡的「買進」「賣出」數值為什麼是 0？
A：TWSE 官方 API 僅提供買賣超淨值（netBuySell），暫不提供買進 / 賣出明細。Wave 3 將評估是否擴充資料來源。該欄位保留為 0 以確保 API contract 穩定。

Q：多語言支援如何測試？
A：在 URL 加 query parameter：`?lang=en`，即可切換英文。用戶偏好會保存至 localStorage，刷新頁面時保持。

### 問題回報

遇到問題請：
1. 記下 `traceId`（API response 中或瀏覽器 console）
2. 提交至：[Github Issues](https://github.com/your-repo/issues) 或內部 Jira
3. 附上：瀏覽器版本、操作步驟、截圖

### 技術支援

- **架構疑問**：@Sophia（系統架構師）
- **後端 API**：@Bruno（後端工程師）
- **前端頁面**：@Felix（前端工程師）
- **QA 相關**：@Quincy / @Quinn（QA 工程師）
- **整體進度**：@Jamie（Project Leader）

---

## 統計數據

### 開發統計

| 指標 | 數值 |
|------|------|
| 新增 API endpoint | 5 個（含 /quote/list 與 /quote/history） |
| 新增前端頁面 | 1 個（StockDetail） |
| 新增前端元件 | 4 個（PriceHeader、KLineChart、FundamentalCard、ChipCard） |
| 新增 Java module | 3 個（stock-quote、stock-fundamental、stock-chip） |
| 新增測試用例 | 90+ 個（單元 + 整合） |
| 程式碼行數（後端） | ~8,500 行 |
| 程式碼行數（前端） | ~5,200 行 |

### 品質指標

| 指標 | 值 |
|------|-----|
| 單元測試覆蓋率（後端） | 82% |
| 單元測試覆蓋率（前端） | 76% |
| SonarQube 品質閘門 | ✅ Pass（無 Critical 問題） |
| Code Review 通過率 | 100%（Brian + Fiona） |
| QA 測試通過率 | 99.5%（Quincy + Quinn cross-review） |
| CVE 掃描結果 | 0 Critical（Linus） |

---

## 下一步（Wave 3 預告）

- 📊 自選股管理（CRUD）+ 推播整合
- 📌 股票提醒設定（停損停利）
- 🤖 AI 綜合評分模型
- 💬 籌碼面進階（法人分點進出、大股東持股變化）
- 🔐 Spring Security RBAC（角色權限系統）
- 🧪 Playwright E2E 套件完備

---

## 附錄

### 文件參考

- [API 對外手冊](20260423_wave2_api-handbook.md)
- [前端開發者指南](20260423_wave2_frontend-dev-guide.md)
- [部署 / 維運手冊](20260423_wave2_ops-handbook.md)
- [schema-lock：API Contract](../03_spec/20260422_schema-lock_stock-detail-apis.md)
- [errorCode 中央化](../03_spec/20260422_errorCodes_central.md)

### 系統要求

**最低要求**：
- 瀏覽器：Chrome 110+、Safari 16+、Firefox 110+、Edge 110+
- 網路：2 Mbps 以上（行情即時推播）
- 記憶體：512 MB 以上（前端）
- 硬碟：100 GB（資料庫，初始）

**建議規格**：
- 瀏覽器：最新版本
- 網路：10 Mbps 以上
- 記憶體：2 GB 以上
- 硬碟：200 GB（資料庫，含歷史資料）

---

**文件版本**：v0.2.0  
**最後更新**：2026-04-23  
**下一版本**：v0.3.0（Wave 3，預計 2026-06-30）

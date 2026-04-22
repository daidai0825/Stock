# 依賴清單追蹤檔 — 台股股票分析平台 v1

- **撰寫者**：Linus（Library 工程師）
- **建立日期**：2026-04-22
- **最後更新**：2026-04-22
- **下一次盤點**：2026-04-29（每週一 09:00）
- **基準文件**：[20260421_initial-evaluation.md](20260421_initial-evaluation.md)

---

## 0. 摘要

| 項目 | 數量 | 備註 |
|------|------|------|
| 後端直接依賴 | **38 個** | 詳見 §1 |
| 前端直接依賴 | **24 個** | 詳見 §2 |
| Maintenance mode 套件 | **3 個** | easy-rules、pushy、lightweight-charts-indicators |
| 商用授權需付費套件 | **0 個** | Highcharts 已排除 |
| GPL 授權套件 | **0 個** | 全數通過合規檢查 |
| 個人單一維護者套件 | **2 個** | pushy、lightweight-charts-indicators |

---

## 1. 後端 Maven 依賴清單（38 個直接依賴）

### 1.1 Spring Boot Core（5 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 1 | spring-boot-starter-web | 3.3.13 | REST Controller、Tomcat embedded | Apache 2.0 | ✓ 活躍（VMware） | 🟢 低 |
| 2 | spring-boot-starter-validation | 3.3.13 | Bean Validation @Valid / @NotBlank | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 3 | spring-boot-starter-actuator | 3.3.13 | health / metrics / prometheus | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 4 | spring-boot-starter-security | 3.3.13 | JWT filter、AuthenticationManager | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 5 | spring-boot-starter-aop | 3.3.13 | AuditAspect 切面 | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.2 ORM 與資料層（3 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 6 | mybatis-spring-boot-starter | 3.0.5 | MyBatis 3.5 整合（支援 Optional） | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 7 | postgresql | 42.7.5 | PostgreSQL 16 JDBC Driver | BSD 2-Clause | ✓ 活躍 | 🟢 低 |
| 8 | flyway-core | 10.20.1 | Schema 遷移（純 SQL） | Apache 2.0 | ✓ 活躍（Redgate） | 🟡 中 — v1.5 評估升級 11.x |

### 1.3 PO / DTO 工具（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 9 | lombok | 1.18.34 | @Getter / @Setter / @Builder | MIT | ✓ 活躍 | 🟢 低 |
| 10 | mapstruct | 1.6.3 | PO ↔ DTO 編譯期 converter | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 11 | mapstruct-processor | 1.6.3 | annotation processor | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 12 | lombok-mapstruct-binding | 0.2.0 | Lombok + MapStruct 共存橋接 | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.4 快取與排程（3 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 13 | spring-boot-starter-data-redis | 3.3.13 | Lettuce client + RedisTemplate | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 14 | shedlock-spring | 5.16.0 | @SchedulerLock 防重複執行 | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 15 | shedlock-provider-redis-spring | 5.16.0 | Redis lock store | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.5 規則引擎（3 個）⚠️ Maintenance Mode

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 16 | easy-rules-core | 4.1.0 | 規則引擎核心 | MIT | 🔴 **Maintenance（5 年無更新）** | 🔴 高 — 已抽 RuleEngine port |
| 17 | easy-rules-mvel | 4.1.0 | YAML / MVEL 表達式支援 | MIT | 🔴 同上 | 🔴 高 |
| 18 | easy-rules-support | 4.1.0 | RulesEngineParameters | MIT | 🔴 同上 | 🔴 高 |

> **緩解措施**：於 `stock-score` 模組封裝 RuleEngine port，預備 RuleBook 0.13 / 自寫 Strategy 兩個替代方案。

### 1.6 推播服務（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 19 | firebase-admin | 9.8.0 | FCM Messaging（Android + Web Push） | Apache 2.0 | ✓ 活躍（Google） | 🟡 中 — Google 不定期 major bump |
| 20 | pushy | 0.15.4 | APNs HTTP/2 + JWT（iOS Web Push） | MIT | ⚠️ **單人維護（jchambers）** | 🔴 高 — 無生產級替代 |
| 21 | telegrambots-longpolling | 9.5.0 | Telegram Long Polling（v1.1） | MIT | ✓ 活躍 | 🟢 低 |
| 22 | telegrambots-client | 9.5.0 | Telegram sendMessage | MIT | ✓ 活躍 | 🟢 低 |

### 1.7 HTTP / 解析 / Rate Limit（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 23 | spring-boot-starter-webflux | 3.3.13 | WebClient（TWSE / OTC / MOPS） | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 24 | jsoup | 1.18.3 | MOPS HTML scraping | MIT | ✓ 活躍 | 🟢 低 |
| 25 | opencsv | 5.10 | TWSE CSV 解析 | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 26 | bucket4j-core | 7.6.0 | Token bucket rate limiter | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.8 工具套件（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 27 | jackson-databind | 2.18.x | JSON 序列化（Spring Boot 內建） | Apache 2.0 | ✓ 活躍 | 🟡 中 — 歷史 CVE 較多需密切追蹤 |
| 28 | jackson-datatype-jsr310 | 2.18.x | LocalDateTime ↔ ISO 8601 | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 29 | guava | 33.4.0-jre | Multimap、Splitter（禁用 Cache） | Apache 2.0 | ✓ 活躍（Google） | 🟢 低 |
| 30 | commons-lang3 | 3.17.0 | StringUtils、ObjectUtils | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.9 編碼工具（1 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 31 | commons-codec | 1.17.1 | Base64 / Hex | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.10 監控（1 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 32 | micrometer-registry-prometheus | 1.13.x | /actuator/prometheus 端點 | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.11 測試（6 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 33 | spring-boot-starter-test | 3.3.13 | JUnit 5 + Mockito + AssertJ | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 34 | testcontainers-postgresql | 1.20.4 | PG 16 Testcontainer | MIT | ✓ 活躍 | 🟢 低 |
| 35 | testcontainers-junit-jupiter | 1.20.4 | @Container 整合 | MIT | ✓ 活躍 | 🟢 低 |
| 36 | archunit-junit5 | 1.3.0 | 模組依賴規則檢查 | Apache 2.0 + BSD | ✓ 活躍 | 🟢 低 |
| 37 | rest-assured | 5.5.0 | API 契約測試 | Apache 2.0 | ✓ 活躍 | 🟢 低 |
| 38 | wiremock-standalone | 3.10.0 | TWSE / MOPS HTTP mock | Apache 2.0 | ✓ 活躍 | 🟢 低 |

### 1.12 後端 License 分布

| License | 數量 | 比例 | 商用合規 |
|---------|------|------|----------|
| Apache 2.0 | 28 | 73.7% | ✓ 完全合規 |
| MIT | 9 | 23.7% | ✓ 完全合規 |
| BSD 2-Clause | 1 | 2.6% | ✓ 完全合規 |
| **GPL / AGPL / LGPL** | **0** | **0%** | ✓ 無 copyleft 風險 |

---

## 2. 前端 npm 依賴清單（24 個直接依賴）

### 2.1 React Core（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 1 | react | 19.2.5 | React 核心 | MIT | ✓ 活躍（Meta） | 🟢 低 |
| 2 | react-dom | 19.2.5 | React DOM 渲染 | MIT | ✓ 活躍 | 🟢 低 |
| 3 | typescript | 5.7.x | TS 編譯器 | Apache 2.0 | ✓ 活躍（Microsoft） | 🟢 低 |
| 4 | @vitejs/plugin-react-swc | 3.7.x | SWC React 編譯 | MIT | ✓ 活躍 | 🟢 低 |

### 2.2 建構工具（1 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 5 | vite | 6.0.x | 開發伺服器 + bundler | MIT | ✓ 活躍（VoidZero） | 🟢 低 |

### 2.3 路由與狀態（3 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 6 | react-router-dom | 6.28.x | Router v6 | MIT | ✓ 活躍（Remix） | 🟡 中 — v7 已釋出 |
| 7 | @tanstack/react-query | 5.62.x | API 資料快取 | MIT | ✓ 活躍 | 🟢 低 |
| 8 | zustand | 5.0.x | 輕量全域狀態 | MIT | ✓ 活躍 | 🟢 低 |

### 2.4 表單與 HTTP（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 9 | react-hook-form | 7.54.x | 表單管理 | MIT | ✓ 活躍 | 🟢 低 |
| 10 | zod | 3.24.x | Schema 驗證 | MIT | ✓ 活躍 | 🟢 低 |
| 11 | @hookform/resolvers | 3.10.x | RHF + Zod 橋接 | MIT | ✓ 活躍 | 🟢 低 |
| 12 | axios | 1.7.9 | HTTP client | MIT | ✓ 活躍 | 🟡 中 — 歷史 CVE 較多需追蹤 |

### 2.5 UI 框架（3 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 13 | antd | 5.23.x | Ant Design 5 | MIT | ✓ 活躍（Ant Group） | 🟡 中 — 5.x patch 變動頻繁 |
| 14 | @ant-design/icons | 5.5.x | Ant Design Icons | MIT | ✓ 活躍 | 🟢 低 |
| 15 | @ant-design/charts | 2.2.x | 一般統計圖 | MIT | ✓ 活躍 | 🟢 低 |

### 2.6 K 線圖（2 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 16 | lightweight-charts | 4.2.x | TradingView K 線核心 | Apache 2.0 | ✓ 活躍（TradingView） | 🟡 中 |
| 17 | lightweight-charts-indicators | 0.3.x | 446 種技術指標 | MIT | ⚠️ **社群維護（deepentropy）** | 🔴 高 — v2 評估自維 fork |

### 2.7 日期與 i18n（3 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 18 | dayjs | 1.11.13 | 日期處理（GMT+8 plugin） | MIT | ✓ 活躍 | 🟢 低 |
| 19 | i18next | 24.x | i18n 核心 | MIT | ✓ 活躍 | 🟢 低 |
| 20 | react-i18next | 15.2.x | React i18n binding | MIT | ✓ 活躍 | 🟢 低 |

### 2.8 測試（4 個）

| # | 套件名 | 鎖定版本 | 用途 | License | 維護狀態 | 風險 |
|---|--------|----------|------|---------|----------|------|
| 21 | vitest | 2.1.x | Unit test runner | MIT | ✓ 活躍 | 🟢 低 |
| 22 | @testing-library/react | 16.1.x | React 測試工具 | MIT | ✓ 活躍 | 🟢 低 |
| 23 | @testing-library/jest-dom | 6.6.x | DOM matchers | MIT | ✓ 活躍 | 🟢 低 |
| 24 | playwright | 1.49.x | E2E 測試 | Apache 2.0 | ✓ 活躍（Microsoft） | 🟢 低 |

### 2.9 前端 License 分布

| License | 數量 | 比例 | 商用合規 |
|---------|------|------|----------|
| MIT | 20 | 83.3% | ✓ 完全合規 |
| Apache 2.0 | 4 | 16.7% | ✓ 完全合規 |
| **GPL / AGPL / LGPL** | **0** | **0%** | ✓ 無 copyleft 風險 |

> 註：msw（mock service worker）為 dev-only，未列入直接依賴計數。

---

## 3. Maintenance Mode 套件追蹤

> 以下三個套件需 Linus 每週掃描 GitHub 活動並於週報揭露

| 套件 | 最後更新 | 維護狀態 | 替代方案 | 切換成本 | 監控指標 |
|------|----------|----------|----------|----------|----------|
| easy-rules-* | 2020-12 | 🔴 5 年無更新、PR 無人 review | RuleBook 0.13 / 自寫 Strategy | 1 週 | GitHub issue 數、Java 21 相容報告 |
| pushy | 隨 release | ⚠️ 單人維護（jchambers） | 自寫 OkHttp + JWT | 5 人天 | jchambers/pushy commits 頻率 |
| lightweight-charts-indicators | 隨 release | ⚠️ 社群維護（deepentropy） | 自維 fork（v2） | 2 週 | npm weekly downloads 趨勢 |

---

## 4. License 合規檢查

### 4.1 合規原則

| License 類別 | 商用授權 | 採用原則 |
|--------------|---------|---------|
| Apache 2.0 / MIT / BSD | ✓ 自由商用 | ✓ 預設可採 |
| LGPL | ⚠️ 條件商用 | ⚠️ 需架構師簽核（避免靜態連結） |
| GPL / AGPL | ❌ 強 copyleft | ❌ **嚴禁混入專有專案** |
| Commercial（商用付費） | ⚠️ 需付費 | ⚠️ 需 Jamie + Dale 簽核 |
| Custom License | ⚠️ 個案評估 | ⚠️ 需法務 review |

### 4.2 v1 合規檢查結論

| 項目 | 結論 |
|------|------|
| 後端 38 個直接依賴 | ✓ **全數通過**（28 個 Apache 2.0、9 個 MIT、1 個 BSD） |
| 前端 24 個直接依賴 | ✓ **全數通過**（20 個 MIT、4 個 Apache 2.0） |
| GPL / AGPL 套件 | ✓ **零** |
| LGPL 套件 | ✓ **零** |
| 商用付費套件 | ✓ **零**（Highcharts 已排除） |
| 風險套件 | ✓ **零** |

### 4.3 遞移依賴 License 風險

> 直接依賴 License 全綠，但**遞移依賴尚未盤點**。首次 OWASP Dependency Check 完成後，需另外用 `mvn license:aggregate-third-party-report` 與 `npx license-checker --production` 產出完整遞移依賴 License 報告。

**待辦**：
- [ ] 後端：`mvn license:aggregate-third-party-report`（Bruno 配合 plugin 加入後執行）
- [ ] 前端：`npx license-checker --production --summary`（Felix 配合執行）
- [ ] 結果整併到本檔案 §4.4

### 4.4 遞移依賴 License 報告（待補）

| 後端遞移依賴 License 分布 | 數量 | 風險 |
|--------------------------|------|------|
| 待 baseline 掃描完成後填寫 | - | - |

| 前端遞移依賴 License 分布 | 數量 | 風險 |
|--------------------------|------|------|
| 待 baseline 掃描完成後填寫 | - | - |

---

## 5. 版本鎖定策略（依 initial-evaluation §5）

### 5.1 後端

- **parent POM**：繼承 `spring-boot-starter-parent:3.3.13`，BOM 集中管理 Spring 系列
- **dependencyManagement**：自寫管理 easy-rules、pushy、telegrambots、firebase-admin 等非 Spring 系列
- **遞移依賴覆蓋**：發現 transitive CVE 時用 `<dependencyManagement>` 強制 pin 版本
- **禁止**：子 module 各自宣告版本

### 5.2 前端

- **強制 commit `package-lock.json`**（Felix PR 必檢）
- 採 `npm ci`（不用 `npm install`）
- v1 暫不引入 Renovate
- 手動更新節奏：每月 1 次 minor / patch、每季 1 次 major 評估

---

## 6. 變更紀錄

| 日期 | 異動 | 異動人 |
|------|------|--------|
| 2026-04-22 | 初版建立，依 initial-evaluation 盤點 38 + 24 個依賴 | Linus |

---

## 7. 下一步行動

| # | 行動 | 對象 | 期限 |
|---|------|------|------|
| 1 | 本清單交 Jamie review | Linus → Jamie | 即時 |
| 2 | 與 Bruno / Felix 確認版本鎖定無誤 | Linus | 本週 |
| 3 | baseline 掃描後補充遞移依賴 License | Linus | 下週 |
| 4 | 每週一同步更新本清單 | Linus | 持續 |

---

**文件結束**

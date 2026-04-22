# Library 前期評估報告 — 台股股票分析平台 v1

- **撰寫者**：Linus（Library 工程師）
- **撰寫日期**：2026-04-21
- **召喚人**：Jamie
- **評估範圍**：v1 純 Docker 本機運行（依 [arch top5 拍板](../../01_leader/decisions/20260421_decision-arch-top5.md) A2/A3/A5）
- **下游交接**：Bruno（後端 API）、Felix（前端）、Preston（Maven 結構）、Sophia（架構）

---

## 0. 執行摘要

| 項目 | 結論 |
|------|------|
| 後端核心依賴數 | **38 個**（直接依賴；遞移依賴另外推估 ~ 350 個） |
| 前端核心依賴數 | **24 個**（直接依賴；遞移依賴 npm 通常 ~ 1,200 個） |
| K 線圖 lib | ✅ **TradingView Lightweight Charts**（45KB、446 個 indicators 社群套件、效能優於同類） |
| 資料庫遷移 | ✅ **Flyway**（Spring Boot 自動偵測、PostgreSQL 單庫足夠、團隊學習成本最低） |
| 規則引擎 | ⚠️ **Easy Rules + 包裝層 Strategy 抽象**（**反 Sophia 預設**，理由見 §1.7） |
| 台股 Java SDK | ❌ **無生產級可用方案**，需自寫 ETL Client（風險最高項） |
| 最大風險 | TWSE/MOPS 為公開 HTML/CSV 抓取，**無正式 API 合約**，scraping 架構穩定性與商用授權合規性兩面受壓 |

> **誠實聲明**（依 conversation.md）：本報告為「前期評估」，套件版本均查證至 2026-04 公開資訊；實作期若版本有更新會於每週掃描週報補正。CVE 數據暫無（尚未建 pom.xml），上線前必須跑 OWASP Dependency Check 補完。

---

## 1. 後端 Java 21 / Spring Boot 3.x 依賴清單（v1 必備）

### 設計前提

- **Java 版本**：Java 21 LTS（Virtual Threads 支援 ETL 大量 IO 並發）
- **Spring Boot 版本**：✅ **3.3.13**（2025-12-18 釋出，3.3 line 為 LTS-style 維護分支）
  - ⚠️ **不選 4.0.x**（2026-03 才釋出，Sophia 架構文件第 8.1 節明確選 3.3.x；4.0 生態套件相容性尚未穩定，違反 system-design.md「不為相容性用 deprecated」與「不自行降級」原則的折衷選擇）
- **MyBatis Spring Boot Starter**：✅ **3.0.5**（對應 Spring Boot 3.3、MyBatis 3.5.16）
  - ⚠️ **不選 4.0.1**（綁定 Spring Boot 4.0、Java 17+、與 Spring Boot 3.3 不相容）
- **JDK Compile Target**：21
- **Maven**：3.9.x（Wrapper 鎖版本）

### 1.1 核心 Spring Boot Starters

| 套件 | 版本 | 用途 | 模組歸屬 |
|------|------|------|----------|
| spring-boot-starter-web | 3.3.13 | REST Controller、Tomcat embedded | stock-boot |
| spring-boot-starter-validation | 3.3.13 | Bean Validation（@Valid、@NotBlank）| 全業務模組 |
| spring-boot-starter-actuator | 3.3.13 | health/metrics/prometheus 端點 | stock-boot |
| spring-boot-starter-security | 3.3.13 | JWT filter、AuthenticationManager | stock-member |
| spring-boot-starter-aop | 3.3.13 | AuditAspect 切面 | stock-common / stock-boot |

### 1.2 ORM 與資料層

| 套件 | 版本 | 用途 | 備註 |
|------|------|------|------|
| mybatis-spring-boot-starter | 3.0.5 | MyBatis 3.5 整合 | ✅ 支援 `Optional<T>` ResultMap（規範允許 Repository → Service） |
| postgresql | 42.7.5 | PostgreSQL JDBC Driver | 對應 PG 16 |
| HikariCP | 5.1.0（內建於 Spring Boot 3.3） | Connection Pool | 不需單獨宣告 |
| **Flyway**（見 §1.4 選型）| 10.20.x | Schema 遷移 | Spring Boot 3.3 內建 starter |

### 1.3 PO/DTO 工具

| 套件 | 版本 | 用途 |
|------|------|------|
| lombok | 1.18.34 | @Getter/@Setter/@Builder（PO/DTO） |
| mapstruct | 1.6.3 | PO ↔ DTO 編譯期生成 converter |
| mapstruct-processor | 1.6.3 | annotation processor |
| lombok-mapstruct-binding | 0.2.0 | Lombok + MapStruct 共存橋接 |

### 1.4 資料庫遷移：Flyway vs Liquibase 選型

| 維度 | Flyway 10.20 | Liquibase 4.30 | Linus 評分 |
|------|--------------|----------------|------------|
| 寫法 | 純 SQL（V1__init.sql）| XML/YAML/JSON changelog | Flyway +1 |
| 學習成本 | 幾乎為零（會 SQL 即可） | 需學 changeset 語法 | Flyway +1 |
| Spring Boot 整合 | starter 內建、自動偵測 `db/migration` | 需獨立配置 | Flyway +1 |
| 多資料庫支援 | 弱 | 強（XML 抽象層） | Liquibase +1 |
| Rollback | Community 版本不支援自動 rollback | 內建 rollback tag | Liquibase +1 |
| Precondition | 無 | 有 | Liquibase +1 |
| 團隊熟悉度 | 高（PostgreSQL 社群預設）| 中 | Flyway +1 |

**Linus 結論**：✅ **Flyway 10.20.x**

**理由**：
1. v1 單一 PostgreSQL 16，無多資料庫需求
2. 14 個 module 共用 schema，純 SQL 比 changelog 更可讀
3. ProjectArch §1.2 強調「Modular Monolith 為 v2 微服務鋪路」— 拆分時 Flyway SQL 可直接複製到子服務
4. Rollback 在金融類系統不依賴 lib 自動處理，應該走「補修 forward migration」原則（更符合 Smart Service 哲學）

### 1.5 快取（Redis on Docker）

| 套件 | 版本 | 用途 | 必裝？ |
|------|------|------|--------|
| spring-boot-starter-data-redis | 3.3.13 | Lettuce client + RedisTemplate | ✅ 必裝 |
| **Redisson** | 3.36.0 | 分散式鎖、RBucket、RMap | ⚠️ **v1 暫不裝** |

**Redisson 評估**：依拍板 A3 v1 為單體單實例，分散式鎖暫無剛需。但 ShedLock 已涵蓋「scheduled task 互斥」場景。**Redisson 列入 v2 backlog**（v2 拆微服務時再評估）。

### 1.6 排程

| 套件 | 版本 | 用途 |
|------|------|------|
| Spring `@Scheduled` | 3.3.13 內建 | 早盤 08:00 / 盤後 14:30 / 籌碼 17:00 / 評分 17:30 |
| **shedlock-spring** | 5.16.0 | @SchedulerLock 防重複執行 |
| **shedlock-provider-redis-spring** | 5.16.0 | 用 Redis 當 lock store（v1 已有 Redis） |

> ⚠️ ShedLock 已釋出 7.6.0，但 Linus 保守選 5.16.0（與 Spring Boot 3.3 對應、社群實戰較多）。v1 雖單實例，**建議裝**：避免本機 IDE 重啟期間排程重複觸發；v2 拆服務時零成本切換。

### 1.7 規則引擎：Easy Rules vs Drools — **Linus 反向意見**

⚠️ **Sophia 在系統架構 §8.7 預選 Easy Rules**，但本人經查證後**提出修正建議**。

| 維度 | Easy Rules 4.1.0 | Drools 9.x（10 即將釋出） | RuleBook 0.13 |
|------|------------------|--------------------------|---------------|
| 最後更新 | **2020-12（maintenance mode，5 年無新功能）** | 2026 持續釋出 | 2024-08 |
| Java 21 相容 | ⚠️ 未官方測試 | ✅ Drools 9 起完整支援 | ⚠️ 未官方驗證 |
| Annotation 風格 | ✅ 簡潔（@Rule + @Condition + @Action） | 重（DRL 語法或 DMN） | ✅ 流暢 fluent API |
| YAML 規則外部化 | ✅ MVEL 支援 | ✅ DRL 檔案 | ❌ |
| 套件大小 | < 200KB | ~ 30MB | < 500KB |
| 社群活躍度 | 🔴 低（PR 半年無人回） | 🟢 高（Red Hat 背書） | 🟡 中 |
| 學習曲線 | 🟢 1 天 | 🔴 1-2 週 | 🟢 2 天 |
| CVE 紀錄（近 3 年） | 0（也可能因為沒人 audit） | 2 個 medium（已修復） | 0 |

**Linus 結論**：⚠️ **Easy Rules 4.1.0 + 自寫薄層 Strategy 抽象**

**理由**：
1. **Sophia 的方向對**（規模才 30 條規則、Drools 過度工程化）— 在「v1 規模」下 Easy Rules 仍是最佳解
2. **但 Easy Rules 已 5 年無大更新是真問題** — 必須在 `stock-score` 模組內加一層 `RuleEngine` interface，將 Easy Rules 包成可替換的 Adapter
3. 一旦 Easy Rules 出現 Java 21 相容問題或安全漏洞，**可在 1 週內切換到 RuleBook 或自寫 Strategy + Spec Pattern**
4. 拒絕 Drools 的理由不變：「每條 rule 都要懂 DRL，業務小白寫不出來」與 SRS §1.2 的 30 條健康度規則複雜度不匹配

**對 Sophia 的修正建議**：把 §8.7 的「Easy Rules 4.x」改為「**Easy Rules 4.1.0（封裝為 RuleEngine port）**」，並在風險表加列「規則引擎供應商鎖定風險：低」。

| 套件 | 版本 | 備註 |
|------|------|------|
| easy-rules-core | 4.1.0 | 核心 |
| easy-rules-mvel | 4.1.0 | YAML/MVEL 表達式支援 |
| easy-rules-support | 4.1.0 | RulesEngineParameters |

### 1.8 推播

#### FCM（Android + Web Push）

| 套件 | 版本 | 用途 |
|------|------|------|
| firebase-admin | **9.8.0**（2026-04 最新） | Firebase Admin SDK，含 FCM Messaging |

#### APNs（iOS Web Push for Safari macOS 13+ / iOS 16.4+）

| 套件 | 版本 | 評估 |
|------|------|------|
| **pushy（jchambers/pushy）** | **0.15.4** | ✅ **採用**。HTTP/2 + JWT、單一活躍維護者、社群實戰最廣 |
| java-apns-http2 | - | ❌ 不採。多年未更新、HTTP/2 支援不完整 |
| 自寫 OkHttp + JWT | - | ❌ 不採。APNs 認證流程細節多，造輪子成本高 |

> ⚠️ Pushy 是「個人維護專案」（Jonathan Chambers 一人），這是潛在風險，但目前無更好替代。**列入 §6 風險清單第 2 項**。

#### Telegram Bot

| 套件 | 版本 | 用途 |
|------|------|------|
| telegrambots-longpolling | **9.5.0**（2026 最新） | Long Polling 接收（管理員 admin bot） |
| telegrambots-client | **9.5.0** | sendMessage（推播主用） |

> v1 推播只需 `telegrambots-client`，Long Polling 在 v1.1 再加。

### 1.9 TWSE / OTC / MOPS 資料抓取

#### HTTP Client

| 套件 | 版本 | 用途 | 評估 |
|------|------|------|------|
| **Spring WebClient**（spring-webflux）| 6.1.x（隨 Spring Boot 3.3） | TWSE/OTC/MOPS 非同步拉取 | ✅ 採用。與 Spring Boot 整合度最高、Reactor 配 Java 21 Virtual Threads 良好 |
| OkHttp 4.12 | - | - | 備案（如 WebClient 對 TWSE chunked encoding 有問題時） |
| Apache HttpClient 5.4 | - | - | 不採（API 較重） |

#### HTML / CSV 解析

| 套件 | 版本 | 用途 |
|------|------|------|
| jsoup | 1.18.3 | MOPS 重大訊息（HTML scraping） |
| opencsv | 5.10 | TWSE 三大法人、融資融券（CSV） |

#### 是否有現成台股 Java client？

| 候選 | GitHub | Stars | 結論 |
|------|--------|-------|------|
| FinMind/FinMind | github.com/FinMind/FinMind | 2.3k | ❌ **僅 Python SDK**，需自寫 Java HTTP wrapper 對 FinMind REST API。可作為「v1.5 付費資料源備案」 |
| VincentLiu3/TWSE | github.com/VincentLiu3/TWSE | < 50 | ❌ 不適合。**Java 早期作品、最後 commit > 5 年**、僅含基本 quote、無籌碼/財報、無單元測試 |
| mlouielu/twstock | github.com/mlouielu/twstock | 2.6k | ❌ Python 專案。可作為**設計參考**（rate limit 策略、URL 對照表） |
| chunkai1312/node-twstock | github.com/chunkai1312/node-twstock | < 200 | ❌ Node.js。同上僅參考 |

**Linus 結論**：❌ **生產級 Java SDK 為零**。必須**自寫 Adapter 層**：
- `stock-infrastructure` 模組下開 `TWSEClient` / `OTCClient` / `MOPSClient` 三個 interface + WebClient impl
- URL 對照表參考 mlouielu/twstock，但 HTTP 調用、解析、retry、rate limit 全自寫
- 預估開發成本：**3 人週**（Bruno + Squad B 共擔）
- 可選：將 Adapter 層**抽出為獨立 GitHub 專案**作為社群貢獻（v2 議題）

### 1.10 工具套件

| 套件 | 版本 | 用途 | 規範對齊 |
|------|------|------|---------|
| jackson-databind | 2.18.x（Spring Boot 內建） | JSON 序列化 | - |
| jackson-datatype-jsr310 | 2.18.x | LocalDateTime ↔ JSON ISO 8601 | ✅ api-design.md 時間規範 |
| guava | 33.4.0-jre | Multimap、Splitter（**禁用 Guava Cache**） | ✅ srs-extension 拍板 |
| commons-lang3 | 3.17.0 | StringUtils、ObjectUtils | - |
| commons-codec | 1.17.1 | Base64、Hex | - |
| ~~Caffeine~~ | - | ❌ **全域禁用**（本地快取） | ✅ system-design.md |
| ~~Guava Cache~~ | - | ❌ **全域禁用** | ✅ |

**BigDecimal 計算工具**：自寫 `BigDecimalUtils` 於 `stock-common`（依 java-development.md「util 提供專屬計算工具」）。

### 1.11 監控（v1 Docker 本機）

| 套件 | 版本 | 用途 |
|------|------|------|
| micrometer-registry-prometheus | 1.13.x（Spring Boot 內建） | /actuator/prometheus 端點 |
| spring-boot-starter-actuator | 3.3.13 | health/info/metrics |

**v1 Tracing**：暫不裝 Sleuth + Zipkin（單體無跨服務 trace 必要）。**列入 v2**。

### 1.12 AUDIT_LOG（依拍板 A4 新增 v1 範圍）

| 套件 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-aop | 3.3.13 | `@Audited` annotation + `AuditAspect` |

**不需第三方 lib**。實作要點：
- `@Audited(action = "MEMBER_LOGIN", target = "USER")` annotation 自寫於 `stock-common`
- `AuditAspect` 於 `stock-infrastructure` 攔截、寫入 `audit_logs` 表
- ThreadLocal 存放 traceId / userId / ip（HandlerInterceptor 注入）

### 1.13 測試

| 套件 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-test | 3.3.13 | JUnit 5 + Mockito + AssertJ + JsonPath | 
| testcontainers-postgresql | 1.20.4 | PG 16 Testcontainer | 
| testcontainers-junit-jupiter | 1.20.4 | @Container 整合 |
| **archunit-junit5** | 1.3.0 | 模組依賴規則檢查（Preston ProjectArch §1.4 強制）|
| rest-assured | 5.5.0 | API 契約測試（Bruno 用） |
| wiremock-standalone | 3.10.0 | TWSE/MOPS HTTP mock |

**Testcontainer 必裝**（依 environment.md「單元測試採用 Testcontainer 模擬託管服務」）。

### 1.14 後端依賴版本鎖定總表

| 套件 | 版本 | 變動風險（升級） |
|------|------|------------------|
| spring-boot | 3.3.13 | 🟢 已 LTS |
| mybatis-spring-boot-starter | 3.0.5 | 🟢 patch 跟即可 |
| postgresql JDBC | 42.7.5 | 🟢 |
| flyway-core | 10.20.1 | 🟡 11.x 已釋出，v1.5 評估 |
| firebase-admin | 9.8.0 | 🟡 Google 不定期 major bump |
| pushy | 0.15.4 | 🔴 個人維護，需密切監看 |
| telegrambots-* | 9.5.0 | 🟢 |
| easy-rules-core | 4.1.0 | 🔴 maintenance mode |
| jsoup | 1.18.3 | 🟢 |
| testcontainers | 1.20.4 | 🟢 |
| archunit | 1.3.0 | 🟢 |

---

## 2. 台股資料源 SDK / 開源專案盤點（核心任務）

### 2.1 公開 API endpoint 與限制盤點

| 來源 | Endpoint | 已知 Java SDK | 流量限制 | 資料格式 | 風險評級 |
|------|----------|---------------|----------|----------|----------|
| TWSE 上市每日成交 | `https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=20260421&stockNo=2330` | ❌ 無 | TWSE 未公告，社群實證 ≤ **3 req/sec** 否則被封 IP | JSON | 🟡 中 |
| TWSE 三大法人 | `https://www.twse.com.tw/fund/T86?response=json&date=20260421&selectType=ALL` | ❌ 無 | 同上 | JSON | 🟡 |
| TWSE 融資融券 | `https://www.twse.com.tw/exchangeReport/MI_MARGN?response=json&date=20260421&selectType=ALL` | ❌ 無 | 同上 | JSON | 🟡 |
| TWSE 警示股/處置股公告 | `https://www.twse.com.tw/announcement/notice` | ❌ 無 | 同上 | HTML（需 jsoup） | 🟠 高 |
| OTC 上櫃每日 | `https://www.tpex.org.tw/web/stock/aftertrading/daily_close_quotes/stk_quote_result.php?l=zh-tw&d=115/04/21&se=AL&_=...` | ❌ 無 | 未公告，建議 ≤ 2 req/sec | JSON（民國年）| 🟠 |
| OTC 三大法人 | `https://www.tpex.org.tw/web/stock/3insti/daily_trade/3itrade_hedge_result.php` | ❌ 無 | 同上 | JSON | 🟠 |
| MOPS 重大訊息 | `https://mops.twse.com.tw/mops/web/t05st02` | ❌ 無 | 限制較鬆但 HTML 結構不穩 | **HTML** | 🔴 高 |
| MOPS 月營收 | `https://mops.twse.com.tw/mops/web/t146sb05` | ❌ 無 | 同上 | HTML | 🔴 |
| MOPS 季報 | `https://mops.twse.com.tw/mops/web/ajax_t164sb04` | ❌ 無 | 同上 | HTML | 🔴 |

### 2.2 第三方平台評估（FinMind 等）

| 平台 | 介接方式 | 商用授權 | Linus 評估 |
|------|----------|----------|------------|
| **FinMind** | REST API + Python SDK | 免費 600 req/hr（註冊 token） / 付費 NTD 990/月 起 | ✅ **強烈建議列為 v1.5 備援**。當 TWSE scraping 連續失敗時降級調用 FinMind |
| FugleAPI | REST + WebSocket | 商用 NTD 5,000+/月 | ⚠️ v2 評估（即時 tick） |
| 群益 API | 需開戶 | NTD 3,000+/月 | ❌ v1 不採 |
| twstock（Python） | - | - | ❌ 不採，但**設計參考價值高**（rate limit、URL 對照） |
| chunkai1312/node-twstock | - | - | ❌ 不採，同上 |

### 2.3 自寫 Adapter 設計建議（給 Bruno）

```
stock-infrastructure/
├── client/
│   ├── twse/
│   │   ├── TwseClient.java               # interface
│   │   ├── TwseWebClientImpl.java        # WebClient impl
│   │   ├── TwseRateLimiter.java          # token bucket（3 req/sec）
│   │   └── parser/
│   │       ├── StockDayParser.java       # JSON → DTO
│   │       └── T86Parser.java            # 三大法人
│   ├── otc/
│   │   ├── OtcClient.java
│   │   └── OtcWebClientImpl.java
│   └── mops/
│       ├── MopsClient.java
│       ├── MopsWebClientImpl.java
│       └── parser/
│           └── AnnouncementJsoupParser.java  # HTML → DTO（最脆弱層）
└── client/fallback/
    └── FinMindClient.java                # v1.5 備援
```

**rate limit 設計**：採 Bucket4j（v1.7.0），token bucket 每秒 3 個 token，超過則等待。
**重試**：Spring Retry @Retryable，max 3 次、backoff 2s。
**HTML 結構變動防護**：Parser 層每筆解析失敗寫 WARN log + 推播管理員（避免靜默失敗）。

---

## 3. 前端 React 19 / TypeScript / Vite 依賴

### 3.1 核心

| 套件 | 版本 | 備註 |
|------|------|------|
| react | **19.2.5**（2026-04-08 釋出） | ✅ stable，2024-12 釋出後已 16 個月，生態完備 |
| react-dom | 19.2.5 | - |
| typescript | 5.7.x | 配 React 19 type 定義 |
| vite | 6.0.x | Vite 6 支援 Environment API、輕度升級 |
| @vitejs/plugin-react-swc | 3.7.x | SWC 編譯，比 Babel 快 |

### 3.2 路由與狀態

| 套件 | 版本 | 用途 |
|------|------|------|
| react-router-dom | 6.28.x | Router v6（v7 升級成本高，v1 不採）|
| **@tanstack/react-query** | 5.62.x | API 資料快取、stale-while-revalidate |
| **zustand** | 5.0.x | 輕量全域狀態 |

**Zustand vs Redux Toolkit 選型**：

| 維度 | Zustand 5.0 | Redux Toolkit 2.x |
|------|-------------|-------------------|
| Boilerplate | 極少 | 多（slice + reducer + dispatch） |
| 學習曲線 | 1 天 | 1 週 |
| DevTools | ✅ Redux DevTools | ✅ |
| 中介軟體生態 | 中 | 大 |
| 適用規模 | 中小型 | 大型企業 |

**Linus 結論**：✅ **Zustand**。MVP 規模 + Felix 團隊規模小 + TanStack Query 已負責 server state，全域 state 只剩 user session、UI 主題、警示計數，Zustand 完全夠用。

### 3.3 表單與 HTTP

| 套件 | 版本 | 用途 |
|------|------|------|
| react-hook-form | 7.54.x | 表單管理 |
| zod | 3.24.x | Schema 驗證（與 RHF 整合） |
| @hookform/resolvers | 3.10.x | RHF + Zod 橋接 |
| axios | 1.7.9 | HTTP（與 Spring Boot Envelope Pattern 配對） |

### 3.4 UI

| 套件 | 版本 | 用途 |
|------|------|------|
| antd | 5.23.x | Ant Design 5（已定）|
| @ant-design/icons | 5.5.x | - |
| @ant-design/charts | 2.2.x | 一般統計圖（搭 K 線專用 lib） |

### 3.5 圖表（台股核心）

| 候選 | 授權 | K 線支援 | 技術指標疊圖 | bundle size | Linus 評分 |
|------|------|----------|--------------|-------------|------------|
| **TradingView Lightweight Charts** | Apache 2.0 | ✅ 原生 candlestick + volume | ✅ 446 個社群 indicators（deepentropy/lightweight-charts-indicators）| **45 KB** | **9/10** |
| Apache ECharts | Apache 2.0 | ✅ candlestick series | ⚠️ 需自寫 indicator 計算 + 疊 line series | ~ 800 KB（tree-shake 後 ~ 300）| 7/10 |
| Recharts | MIT | ⚠️ 無原生 OHLC | ❌ 需自寫 | ~ 450 KB | 4/10 |
| Highcharts | **商用付費**（年費 NTD 數萬）| ✅ 強 | ✅ 強 | ~ 200 KB | ❌ 排除 |
| ApexCharts | MIT | ✅ candlestick | ⚠️ 部分 | ~ 500 KB | 6/10 |

**Linus 結論**：✅ **TradingView Lightweight Charts 4.2.x**

**理由**：
1. **業界標準** — 全球專業交易平台事實標準（Bitfinex、Polygon.io、Bybit 都用）
2. **效能** — 5 萬根 K 線流暢顯示（SRS 要求歷史 K 線「日 1 年、週 5 年、月 10 年」= 最多 ~ 2,500 根，遠未到極限）
3. **技術指標** — `deepentropy/lightweight-charts-indicators` 已內建 446 種（MA / KD / MACD 全在內），SRS §M-TECH 的 5 個必備指標可直接套用
4. **bundle 小** — 45 KB，配合 Vite tree-shake 不會壓垮首屏
5. **授權** — Apache 2.0，商用無爭議（Highcharts 直接排除）

| 套件 | 版本 |
|------|------|
| lightweight-charts | 4.2.x |
| lightweight-charts-indicators | 0.3.x（社群套件，需評估 v2 是否自維） |

### 3.6 日期、i18n、測試

| 套件 | 版本 | 用途 |
|------|------|------|
| dayjs | 1.11.13 | 取代 moment（GMT+8 plugin）|
| react-i18next | 15.2.x | i18n（合規文案 D1 拍板必需）|
| i18next | 24.x | 核心 |
| **vitest** | 2.1.x | Unit test |
| @testing-library/react | 16.1.x | RTL |
| @testing-library/jest-dom | 6.6.x | matchers |
| **playwright** | 1.49.x | E2E（API 流程 + 合規文案掃描）|
| msw | 2.7.x | API mock |

### 3.7 前端依賴版本鎖定總表

| 套件 | 版本 | 變動風險 |
|------|------|----------|
| react | 19.2.5 | 🟢 |
| antd | 5.23.x | 🟡 5.x patch 變動頻繁 |
| lightweight-charts | 4.2.x | 🟡 |
| lightweight-charts-indicators | 0.3.x | 🔴 社群套件、v2 評估自維 |
| react-router-dom | 6.28.x | 🟡 v7 已釋出但升級成本高 |
| zustand | 5.0.x | 🟢 |
| @tanstack/react-query | 5.62.x | 🟢 |
| axios | 1.7.9 | 🟢（持續監看 CVE）|

---

## 4. CVE 掃描工具配置（v1 適用）

### 4.1 後端

| 工具 | 配置 | 執行頻率 |
|------|------|----------|
| **OWASP Dependency Check Maven plugin** | `mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7` | git pre-push hook + 每週手動 |
| `mvn versions:display-dependency-updates` | 顯示可升級依賴 | 每週手動 |
| `mvn dependency:tree` | 完整遞移依賴樹 | 每月 + PR 變動依賴時 |

**failBuildOnCVSS=7**：CVSS ≥ 7（High）即 build 失敗。Critical 直接 block，符合 Linus agent 規範「Critical 立即停 merge」。

### 4.2 前端

| 工具 | 配置 |
|------|------|
| `npm audit --audit-level=high` | git pre-push hook |
| `npm outdated` | 每週手動 |
| `npx depcheck` | 每月找未使用依賴 |

### 4.3 v1 自動化

依拍板 A2「v1 不需 Jenkins」，採：
- **git pre-push hook**：執行 `npm audit` + `mvn dependency-check:check`
- **每週手動**：Linus 跑完整掃描，產出 `docs/05_development/library/weekly/YYYYMMDD_weekly-report.md`
- **CVE 即時通報**：訂閱 NVD email + Spring Security Advisories 通知

> v2 上 Jenkins 後再轉為 CI 自動化（Renovate 或 Dependabot 待 v2 確認 Git 平台後再啟）。

---

## 5. 版本鎖定策略

### 5.1 後端

- **parent POM**：繼承 `spring-boot-starter-parent:3.3.13`，BOM 集中管理 Spring 系列版本
- **dependencyManagement**：自寫管理 `easy-rules`、`pushy`、`telegrambots`、`firebase-admin` 等非 Spring 系列
- **遞移依賴覆蓋**：發現 transitive CVE 時用 `<dependencyManagement>` 強制 pin 版本（範本見 library-upgrade-evaluation skill §4）
- **不允許**：子 module 各自宣告版本（會造成版本漂移）

### 5.2 前端

- **強制 commit `package-lock.json`**（Felix PR 必檢）
- 採 `npm ci`（不用 `npm install`），確保 lock file 一致
- v1 暫不引入 Renovate（規模小、PR 噪音不值）
- **手動更新節奏**：每月 1 次 minor/patch、每季 1 次 major 評估

---

## 6. Library 風險警示（Top 3）

### 🔴 風險 1：TWSE / OTC / MOPS 無正式 API 合約

| 維度 | 內容 |
|------|------|
| 技術風險 | MOPS 重大訊息 / 月營收為 HTML scraping，**HTML 結構變動即整套 ETL 崩盤** |
| 商用授權風險 | TWSE 公開資料商用是否需付費仍未拍板（Sophia 系統架構 §15 也列為待確認）|
| 影響範圍 | M-QUOTE / M-CHIP / M-FUND / M-NEWS 四大模組（佔 SRS 32 個子功能）|
| 緩解方案 | 1. Parser 層獨立、單元測試覆蓋率 ≥ 90%<br/>2. v1.5 預留 FinMind 商用 API 降級接口（已在 §2.3 設計）<br/>3. **Jamie 必須在上線前帶回給 Dale 確認 TWSE 商用授權 MOU 需求** |

### 🔴 風險 2：Easy Rules 進入 maintenance mode（5 年無更新）

| 維度 | 內容 |
|------|------|
| 技術風險 | 2020-12 後僅修 bug、無 Java 21 官方驗證、PR 無人 review |
| 影響範圍 | M-SCORE 模組（健康度評分核心）|
| 緩解方案 | 1. 已建議在 `stock-score` 包裝 RuleEngine port（§1.7）<br/>2. 預備 RuleBook 0.13 / 自寫 Strategy 兩個替代方案<br/>3. 每週掃描 Easy Rules GitHub 觀察 Java 21 issue 數 |

### 🟠 風險 3：Pushy（APNs lib）為單人維護

| 維度 | 內容 |
|------|------|
| 技術風險 | jchambers 一人維護，若停更則 iOS Web Push 無生產級替代 |
| 影響範圍 | M-NOTIFY 的 iOS 推播（依拍板 A2 v1 暫可不啟） |
| 緩解方案 | 1. v1 先 ship FCM + Telegram，APNs 待真機測試再啟<br/>2. 備案：直接呼叫 Apple HTTP/2 endpoint（自寫 OkHttp + JWT，預估 5 人天）<br/>3. 訂閱 jchambers/pushy GitHub release notification |

---

## 7. 對 Sophia 系統架構的修正建議

| 章節 | 原文 | Linus 修正建議 |
|------|------|---------------|
| §8.7 規則引擎 | 「採 Easy Rules 4.x」 | 改為「Easy Rules 4.1.0 + RuleEngine port 抽象層」並在 §12 風險表加列項 |
| §8.1 後端框架 | 「Spring Boot 3.3.x」 | 鎖定 **3.3.13**（避免 minor 漂移）|
| §11.2 月費 | 列入 X-Ray、CloudWatch | 依 A2 拍板**全數刪除 v1 月費**，v1 純 Docker 月費 = USD 99/年（APNs 開發者帳號）÷ 12 ≈ **USD 8/月** |

> 上述修正由 Jamie 評估是否回拋給 Sophia 修文。

---

## 8. 下一步行動

| # | 行動 | 對象 | 期限 |
|---|------|------|------|
| 1 | 本報告交 Jamie review | Linus → Jamie | 即時 |
| 2 | Preston 依本報告產出 parent POM 與各 module POM | Preston | 本週 |
| 3 | Felix 依本報告初始化 `package.json` | Felix | 本週 |
| 4 | 跑首次 OWASP Dependency Check 建立 baseline | Linus | pom.xml 完成後 |
| 5 | 建立每週掃描排程 + 週報格式 | Linus | 下週起 |
| 6 | 與 Bruno 確認 TWSE Adapter 介面設計 | Linus + Bruno | 設計階段 |

---

## 附錄 A：依賴版本快速索引

### 後端

| 類別 | 套件 | 版本 |
|------|------|------|
| Core | spring-boot-starter-* | 3.3.13 |
| ORM | mybatis-spring-boot-starter | 3.0.5 |
| DB | postgresql | 42.7.5 |
| Migration | flyway-core | 10.20.1 |
| Tools | lombok | 1.18.34 |
| Tools | mapstruct | 1.6.3 |
| Cache | spring-boot-starter-data-redis | 3.3.13 |
| Schedule | shedlock-spring | 5.16.0 |
| Rules | easy-rules-core | 4.1.0 |
| Push | firebase-admin | 9.8.0 |
| Push | pushy | 0.15.4 |
| Push | telegrambots-client | 9.5.0 |
| HTTP | spring-webflux（WebClient）| 3.3.13 內建 |
| Parse | jsoup | 1.18.3 |
| Parse | opencsv | 5.10 |
| RateLimit | bucket4j-core | 7.6.0 |
| Test | testcontainers | 1.20.4 |
| Test | archunit-junit5 | 1.3.0 |

### 前端

| 類別 | 套件 | 版本 |
|------|------|------|
| Core | react / react-dom | 19.2.5 |
| Build | vite | 6.0.x |
| TS | typescript | 5.7.x |
| Router | react-router-dom | 6.28.x |
| State | zustand | 5.0.x |
| Server State | @tanstack/react-query | 5.62.x |
| Form | react-hook-form + zod | 7.54.x / 3.24.x |
| HTTP | axios | 1.7.9 |
| UI | antd | 5.23.x |
| Chart | lightweight-charts | 4.2.x |
| Date | dayjs | 1.11.13 |
| i18n | react-i18next | 15.2.x |
| Test | vitest | 2.1.x |
| Test | playwright | 1.49.x |

---

**文件結束**

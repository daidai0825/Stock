# 台股股票分析平台 (Taiwan Stock Analysis Platform)

> 為散戶投資人設計的響應式 Web 股票分析平台
> 
> v1 MVP 預計 2026 Q3/Q4 上線

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-45%25-yellow)
![License](https://img.shields.io/badge/license-proprietary-blue)
![Version](https://img.shields.io/badge/version-v1.0.0--alpha-lightgray)

---

## ✨ 功能特色（v1 MVP）

### 已實作（Wave 1 + Wave 2 + Wave B）

- **即時行情查詢**：台灣證交所（TWSE）上市股票 + OTC 上櫃股票，T+1 收盤資料
- **個股詳情頁**：K 線圖（日/週/月線切換，使用 TradingView Lightweight Charts）、基本面分析（EPS、PER、PBR、ROE）、三大法人買賣超籌碼分析
- **行情數據來源**：
  - 上市行情：TWSE 公開資訊觀測站
  - 基本面：MOPS（公開資訊觀測站）
  - 籌碼資料：TWSE 三大法人買賣
- **會員管理**：註冊、登入、個人資料修改、會話管理（JWT Access Token）
- **自選股清單**（Wave B）：加入、移除、查看自選股列表
- **響應式設計**：完整支援手機、平板、桌機

### 規劃中（Wave C/D）

- 完整 Refresh Token 與 Access Token 自動更新
- Email 信箱驗證流程
- 密碼重設、帳號註銷
- Telegram / FCM 推播通知
- 多語系支援（英文、繁體中文）

---

## 🏗️ 技術棧

| 層級 | 技術 | 版本 |
|------|------|------|
| **後端語言** | Java | 21 (LTS) |
| **後端框架** | Spring Boot | 3.3.13 |
| **ORM** | MyBatis | 3.5+ |
| **資料庫** | PostgreSQL | 16 |
| **快取** | Redis | 7 |
| **前端框架** | React | 18+ |
| **前端語言** | TypeScript | 5.x |
| **前端構建** | Vite | 5.x |
| **UI 元件庫** | Ant Design | 5.x |
| **HTTP 客戶端** | Axios | 1.x |
| **圖表庫** | TradingView Lightweight Charts | 4.x |
| **狀態管理** | Zustand | 4.x |
| **資料獲取** | React Query | 5.x |
| **測試（後端）** | JUnit 5 + Mockito + Testcontainers | - |
| **測試（前端）** | Vitest + React Testing Library | - |
| **E2E 測試** | Playwright | - |
| **API 文件** | OpenAPI 3.0 (Swagger) | - |
| **容器化** | Docker + Docker Compose | - |
| **CI/CD** | Jenkins + Declarative Pipeline | - |
| **部署** | AWS ECS/EKS + Kubernetes (地端) | - |

---

## 📁 專案結構

```
Stock/
├── README.md                           # 本檔案
├── CLAUDE.md                          # 開發團隊規範 & Agent 定義
├── docker-compose.yml                 # 本機開發環境
├── backend/                           # Java Spring Boot 後端
│   ├── pom.xml                       # Maven parent POM
│   ├── stock-platform/               # parent module（鎖定版本）
│   ├── stock-common/                 # 共用元件（ApiResponse、ErrorCode、GlobalExceptionHandler、TraceId）
│   ├── stock-domain/                 # PO 實體（User、UserPreference、AuditLog）
│   ├── stock-infrastructure/         # Redis、Event Publisher、外部 API Client
│   ├── stock-member/                 # 會員管理（登入、註冊、個人資料）
│   ├── stock-quote/                  # 行情查詢（K 線、歷史行情）
│   ├── stock-fundamental/            # 基本面分析（EPS、PER、PBR、ROE）
│   ├── stock-chip/                   # 籌碼分析（三大法人買賣超）
│   ├── stock-watchlist/              # 自選股（placeholder）
│   ├── stock-notify/                 # 通知服務（placeholder）
│   ├── stock-boot/                   # Spring Boot 啟動類 & 配置
│   └── ... (9 個 placeholder module)
├── frontend/                          # React + TypeScript 前端
│   ├── src/
│   │   ├── components/               # 可複用元件
│   │   ├── pages/                    # 路由頁面
│   │   │   ├── Home/                # 首頁
│   │   │   ├── StockDetail/         # 個股詳情頁
│   │   │   ├── Watchlist/           # 自選股清單
│   │   │   └── Login/               # 登入頁
│   │   ├── hooks/                    # 自訂 Hook（useStockQuote、useStockHistory 等）
│   │   ├── services/                # API 呼叫層（stockService、memberService）
│   │   ├── stores/                  # Zustand 全域狀態（authStore）
│   │   ├── types/                   # TypeScript 型別定義
│   │   ├── constants/               # 常數（errorCodes、routes）
│   │   ├── i18n/                    # 多語系翻譯
│   │   ├── mocks/                   # MSW mock handlers（開發用）
│   │   ├── utils/                   # 工具函式
│   │   ├── App.tsx                  # 主應用元件
│   │   └── main.tsx                 # Vite 進入點
│   ├── public/                       # 靜態資源
│   ├── package.json
│   └── vite.config.ts
└── docs/                             # 技術文件與決策記錄
    ├── 01_leader/                   # Jamie 進度報告 & 決策
    ├── 02_product/                  # Patricia 產品需求 (PRD)
    ├── 03_spec/                     # Peter 系統規格 (SRS) & schema-lock
    ├── 04_architecture/             # Sophia/Preston 架構設計
    ├── 05_development/              # 開發工程師筆記 (Bruno/Felix)
    ├── 06_review/                   # 審查報告 (Fiona/Brian)
    ├── 07_qa/                       # 測試報告 (Quincy/Quinn)
    ├── 08_deployment/               # Jenkins Pipeline & 部署文件
    └── 09_documentation/            # Daisy 統整的對外文件
```

---

## 🚀 快速開始

### 前置需求

- **Java 21** (Oracle JDK 或 Eclipse Temurin)
- **Node.js 20+** (LTS)
- **Docker + Docker Compose**（用於本機資料庫）
- **Git 2.30+**
- **Maven 3.9+**（或使用 Maven Wrapper）

### 本機開發步驟

#### 1. Clone 專案

```bash
git clone https://github.com/daidai0825/Stock.git
cd Stock
```

#### 2. 啟動本機資料庫 & 快取

```bash
docker compose up -d
```

此命令啟動：
- PostgreSQL 16（資料庫：`stockdb`，帳密：`stockuser/stockpass`）
- Redis 7（快取）
- Adminer（DB 管理 UI，http://localhost:8081）

#### 3. 後端啟動

```bash
cd backend

# 方式 A：IDE 直接執行（推薦，Maven Wrapper 設定中）
# 在 IntelliJ IDEA 或 VS Code 直接 Run `StockPlatformApplication`

# 方式 B：命令行
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# 或使用 Maven Wrapper（Windows）
mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

後端預設監聽：http://localhost:8080

#### 4. 前端啟動

```bash
cd frontend
npm install
npm run dev
```

前端預設監聽：http://localhost:5173

#### 5. 驗證環境

- **後端健康檢查**：http://localhost:8080/actuator/health
- **前端首頁**：http://localhost:5173
- **Swagger UI**（dev 環境）：http://localhost:8080/swagger-ui.html
- **Adminer（DB 管理）**：http://localhost:8081

---

## ⚙️ 環境變數

### 後端 Local 配置

`backend/stock-boot/src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/stockdb
    username: stockuser
    password: stockpass
  redis:
    host: localhost
    port: 6379

stock:
  jwt:
    secret: local-dev-secret-must-be-at-least-32-chars-long-xyz
```

> Local 可明文密碼（規範允許），但禁止複製到 dev/prod

### 前端 Local 配置

`frontend/.env.local`：

```
VITE_API_BASE_URL=http://localhost:8080
VITE_LOG_LEVEL=debug
VITE_MSW_ENABLED=false
```

---

## 🧪 測試

### 後端測試

```bash
cd backend

# 運行所有測試
./mvnw test

# 運行特定模組測試
./mvnw test -pl stock-member

# 運行整合測試（Testcontainers）
./mvnw verify
```

### 前端測試

```bash
cd frontend

# 運行單元測試（Vitest）
npm run test

# 運行帶覆蓋率
npm run test:coverage

# E2E 測試（Playwright）
npm run test:e2e
```

---

## 📚 文件導覽

### 使用者文件

- [使用者手冊](docs/09_documentation/USER_GUIDE.md) — 散戶投資人如何使用平台
- [疑難排解](docs/09_documentation/TROUBLESHOOTING.md) — 常見問題與解決方案

### 開發者文件

- [API 文件](docs/03_spec/20260422_schema-lock_stock-detail-apis.md) — REST API 完整契約
- [架構說明](docs/04_architecture/system/) — 系統架構、模組劃分、設計決策
- [開發規範](CLAUDE.md) — 代碼風格、工作流程、測試規範
- [錯誤碼註冊表](docs/03_spec/20260422_errorCodes_central.md) — 業務錯誤碼定義

### 運維文件

- [部署說明](docs/08_deployment/) — Jenkins Pipeline、環境配置、部署流程
- [維運手冊](docs/09_documentation/MAINTENANCE.md) — 監控、告警、備份、災難復原

### 產品文件

- [產品需求](docs/02_product/) — PRD、功能設計、業務規則
- [系統規格](docs/03_spec/20260421_SRS_stock-analysis-mvp.md) — 完整需求規格書

### 進度與決策

- [進度報告](docs/01_leader/progress/) — Jamie 每階段完成摘要
- [決策紀錄](docs/01_leader/decisions/) — 跨團隊決策與投票記錄
- [技術債清單](docs/01_leader/td-tracking/20260422_TD-list-consolidated.md) — 待處理技術改進項

---

## 👥 開發團隊

本專案採用 **全端開發團隊**（Team V2）架構，13 位工程師分工：

| 角色 | 姓名 | 職責 |
|------|------|------|
| Leader（唯一窗口） | Jamie | 進度管理、跨團隊協調、決策仲裁 |
| 資深 PM | Patricia | 產品功能構想、需求分析 |
| PM | Peter | API 規格、契約定義、SRS 文件 |
| 系統架構師 | Sophia | 跨系統整合、雲端架構、技術選型 |
| 專案架構師 | Preston | 模組劃分、套件結構、設計模式 |
| Library 工程師 | Linus | 版本管控、CVE 監控、依賴評估 |
| 資深前端工程師 | Felix | React/TypeScript 開發 |
| 資深後端工程師 | Bruno | Java/Spring 開發 |
| 資深前端 Reviewer | Fiona | 前端程式碼審查 |
| 資深後端 Reviewer | Brian | 後端程式碼審查 |
| 資深 QA #1 | Quincy | 功能測試、自動化效能測試 |
| 資深 QA #2 | Quinn | 功能測試、自動化效能測試 |
| 技術文件員 | Daisy | 全程跟進、技術文件統整 |

詳見 [CLAUDE.md](CLAUDE.md)。

---

## ⚖️ 法律聲明

### 投資免責聲明

本平台僅提供股票行情查詢、基本面分析、籌碼追蹤等資訊呈現功能，**不構成任何投資建議**。

投資人應自行判斷風險，進行投資決策前建議：
- 諮詢專業財務顧問
- 充分了解個人風險承受度
- 查閱相關法規與上市櫃公司公開資訊

**本平台與其開發者對任何投資損失不負法律責任。**

### 資料來源

- **行情資料**：台灣證交所（TWSE）、櫃買中心（OTC）
- **基本面資料**：公開資訊觀測站（MOPS）
- **籌碼資料**：台灣證交所（TWSE）

資料延遲、準確性等視各資料源而定，使用者應自行驗證。

### 個人資訊保護

本平台依台灣個人資料保護法進行資料保護。使用者帳號、偏好設定等資訊在 PostgreSQL 加密儲存，不會用於任何第三方廣告或商業目的。

---

## 📄 授權

本專案代碼為 **Proprietary**（專有）。未經授權，禁止複製、修改、發布。

---

## 🔗 相關連結

- **GitHub 倉庫**：https://github.com/daidai0825/Stock
- **Jenkins Pipeline**：待部署後提供
- **API 文件（Swagger）**：http://localhost:8080/swagger-ui.html（開發環境）
- **監控面板**：待配置

---

## 📞 支援與聯繫

- **技術問題**：建立 GitHub Issue
- **功能建議**：聯繫產品團隊（Patricia）
- **緊急問題**：通知 Jamie（Leader）

---

## 📝 版本歷史

### v1.0.0-alpha（2026-04-22）

**Stage 8 Dual Review 進行中**

- ✅ Wave 1：基礎建設 & 會員管理（後端）
- ✅ Wave 2：行情、基本面、籌碼分析（後端）
- ✅ Wave A：首頁、登入頁、自選股清單（前端）
- ✅ Wave B：個股詳情頁 & K 線圖（前端）
- 🔄 Stage 8：Dual Code Review（後端 Brian、前端 Fiona）
- 📋 Wave C：Refresh Token、Email 驗證、通知服務
- 📋 Wave D：多語系、效能優化、上線準備

---

**最後更新**：2026-04-22  
**維護人**：Daisy（技術文件員）、Jamie（Leader）

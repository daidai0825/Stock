# Stock Platform Backend

台股股票分析與追蹤平台後端（Modular Monolith / Spring Boot 3.3.13 / Java 21 / PostgreSQL 16 / Redis 7）。

> 階段：**Stage 4 / Wave 1**（基礎建設與會員骨架）。本版交付 Maven 14 module 結構、stock-common 全套基礎設施、M-MEMBER 5 支 API、Flyway migrations 與 docker-compose 開發環境。

---

## 啟動步驟（local profile）

### 1. 啟動託管服務（PostgreSQL + Redis + Adminer）

```bash
cd /usr/local/dale/daidai0825/Stock
docker-compose up -d
```

| 服務 | 連線方式 |
|------|----------|
| PostgreSQL 16 | `localhost:5432` / db=`stockdb` / user=`stockuser` / pass=`stockpass` |
| Redis 7 | `localhost:6379`（無密碼） |
| Adminer | http://localhost:8081 （System=PostgreSQL, Server=postgres） |

### 2. 建置與測試

使用 **Maven Wrapper**（推薦，無須本機安裝 Maven）：

```bash
cd /usr/local/dale/daidai0825/Stock/backend
./mvnw clean install -DskipTests
./mvnw test
```

或使用系統 Maven（若已安裝）：

```bash
mvn clean install -DskipTests
mvn test
```

首次執行 `./mvnw` 時，會自動下載 Maven 3.9.9 到 `.mvn/wrapper/` 目錄。

### 3. 由 IDE 啟動

啟動類別：`tw.com.stockplatform.boot.StockPlatformApplication`
預設 profile：`local`（讀 `application-local.yml`）

啟動後：
- API 根：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html
- Actuator health：http://localhost:8080/actuator/health

Flyway 會自動執行 `V1.0.0` / `V1.0.1` migrations 建立 `users` / `user_preferences` / `audit_logs` 三張表。

---

## Maven Module 對照表

| Module | 對應 SRS | 狀態 |
|--------|----------|------|
| `stock-common` | - | ✅ Wave 1 完整實作（ApiResponse、ErrorCode、Audit、TraceId） |
| `stock-domain` | - | ✅ Wave 1 落地 USER/USER_PREFERENCE/AUDIT_LOG PO + UserStatus enum |
| `stock-infrastructure` | - | ✅ Wave 1 落地 Redis config、AuditLogMapper、AsyncConfig |
| `stock-member` | M-MEMBER | ✅ Wave 1 完整實作（5/10 支 API） |
| `stock-watchlist` | M-WATCH | 🟡 Wave 2 — Wave 1 placeholder |
| `stock-quote` | M-QUOTE | 🟡 Wave 2 |
| `stock-technical` | M-TECH | 🟡 Wave 2 |
| `stock-chip` | M-CHIP | 🟡 Wave 2 |
| `stock-fundamental` | M-FUND | 🟡 Wave 2 |
| `stock-news` | M-NEWS | 🟡 Wave 2 |
| `stock-risk` | M-RISK | 🟡 Wave 3 |
| `stock-score` | M-SCORE | 🟡 Wave 3 |
| `stock-notify` | M-NOTIFY | 🟡 Wave 3 |
| `stock-boot` | - | ✅ Wave 1 啟動裝配（含 Flyway migrations） |

---

## API 範例（curl）

### 註冊

```bash
curl -X POST http://localhost:8080/api/v1/member/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@stockplatform.tw",
    "password": "demo12345",
    "displayName": "Demo 使用者"
  }'
```

回應：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "xxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "email": "demo@stockplatform.tw",
    "displayName": "Demo 使用者",
    "status": "ACTIVE",
    "createdAt": "2026-04-22T10:30:00"
  },
  "timestamp": "2026-04-22T10:30:00.123+08:00",
  "traceId": "..."
}
```

### 登入

```bash
curl -X POST http://localhost:8080/api/v1/member/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@stockplatform.tw",
    "password": "demo12345"
  }'
```

### 查詢個人資料（需 Bearer Token）

```bash
TOKEN=eyJhbGciOiJIUzI1NiJ9...
curl -X POST http://localhost:8080/api/v1/member/profile/get \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 更新個人資料

```bash
curl -X POST http://localhost:8080/api/v1/member/profile/update \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "New Name",
    "notifyTelegramEnabled": true
  }'
```

### 登出

```bash
curl -X POST http://localhost:8080/api/v1/member/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## 全域規範要點

- **HTTP 狀態碼**：永遠 200，業務狀態以 `code` 表示（Envelope Pattern）。
- **錯誤碼分段**：1xxx 參數、2xxx 業務、3xxx 權限、4xxx 資源、5xxx 第三方、9xxx 系統。
- **時區**：GMT+8 固定（`Asia/Taipei`）。
- **時間欄位**：`LocalDateTime`，不帶時區（資料庫 `TIMESTAMP WITHOUT TIME ZONE`）。
- **ID**：UUID 字串，欄位寬 `VARCHAR(36)`。
- **數值**：一律 `BigDecimal`，禁用 `double` / `float`。
- **Optional**：僅 Repository → Service / Enum lookup 場景使用，DTO/PO/REST 回傳禁止。

詳細規範見 `/usr/local/dale/daidai0825/Stock/.claude/rules/`。

---

## Wave 1 已知技術債

詳見：`/usr/local/dale/daidai0825/Stock/docs/05_development/backend/20260422_wave1_skeleton.md`

主要項目：
- JWT 黑名單與 Refresh Token（待 Wave 2 接 Redis）
- Spring Security Filter Chain（目前由 Controller 自行解 token）
- Email 驗證流程（register 暫時直接設 ACTIVE）
- ArchUnit 模組依賴規則測試（待 Wave 2 補）

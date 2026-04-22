# Wave 1 開發筆記 — 基礎建設與會員骨架

- **撰寫者**：Bruno（Senior Backend Engineer）
- **撰寫日期**：2026-04-22
- **召喚人**：Jamie
- **任務範圍**：Stage 4 / Wave 1
- **參考文件**：
  - SRS：[20260421_SRS_stock-analysis-mvp.md](../../03_spec/20260421_SRS_stock-analysis-mvp.md)
  - 專案架構：[20260421_ProjectArch_stock-backend.md](../../04_architecture/project/20260421_ProjectArch_stock-backend.md)
  - Library 評估：[20260421_initial-evaluation.md](../library/20260421_initial-evaluation.md)
  - 平台拍板：[20260422_decision-platform-form.md](../../01_leader/decisions/20260422_decision-platform-form.md)

---

## 1. 已完成項目

### 1.1 Maven 結構（14 module + 1 parent）

| Module | 內容 | 狀態 |
|--------|------|------|
| `stock-platform` (parent) | 鎖定 Spring Boot 3.3.13 / Java 21；集中 dependencyManagement | ✅ |
| `stock-common` | ApiResponse、ErrorCode、BusinessException、GlobalExceptionHandler、TimeUtils、BigDecimalUtils、TraceIdFilter、Audited、AuditAspect、AuditEvent(Publisher) | ✅ |
| `stock-domain` | UserPO、UserPreferencePO、AuditLogPO、UserStatus enum | ✅ |
| `stock-infrastructure` | RedisConfig、AsyncConfig、AuditLogMapper、AuditEventPublisherImpl | ✅ |
| `stock-member` | MemberController + 5 支 API + Service/Mapper/Convertor + JWT/BCrypt | ✅ |
| `stock-watchlist` ~ `stock-notify` (9 個) | placeholder Controller（回 9001 + 套件結構 .gitkeep） | 🟡 骨架 |
| `stock-boot` | StockPlatformApplication + 4 個 application*.yml + Flyway 兩支 migrations | ✅ |

### 1.2 Docker Compose 開發環境

`/usr/local/dale/daidai0825/Stock/docker-compose.yml`：
- `postgres:16-alpine`（DB=`stockdb`、預設帳密 `stockuser/stockpass`、TZ=Asia/Taipei）
- `redis:7-alpine`（appendonly=yes）
- `adminer:4`（DB 管理 UI，port=8081）
- 應用 service 暫不放（IDE 啟動）

### 1.3 stock-common 核心元件

| 元件 | 路徑 | 說明 |
|------|------|------|
| `ApiResponse<T>` | `common/response/ApiResponse.java` | Envelope Pattern；`success / fail` 兩個 factory |
| `ErrorCode` | `common/constant/ErrorCode.java` | 41 個常用碼（涵蓋 1xxx-9xxx）+ `fromCode(int)` Optional 反查 |
| `BusinessException` | `common/exception/BusinessException.java` | 帶 code 的 RuntimeException |
| `GlobalExceptionHandler` | `common/exception/handler/` | `@RestControllerAdvice`，**HTTP 200 + 業務碼**；含 Bean Validation 欄位錯誤組裝 |
| `TimeUtils` | `common/util/TimeUtils.java` | GMT+8 固定時區工具 |
| `BigDecimalUtils` | `common/util/BigDecimalUtils.java` | safeAdd/Subtract/Multiply/Divide/scale，null safe |
| `TraceIdFilter` | `common/trace/TraceIdFilter.java` | `X-Trace-Id` 注入 MDC，回傳同名 header |
| `@Audited` + `AuditAspect` + `AuditEventPublisher` | `common/audit/` | 切面攔註解、組事件、交由 Infra 層非同步寫入 audit_logs |

### 1.4 M-MEMBER 5 支 API

| # | 路徑 | Controller method | 認證 | Audited |
|---|------|-------------------|------|---------|
| 1 | POST `/api/v1/member/register` | `register` | 否 | ✅ MEMBER_REGISTER |
| 2 | POST `/api/v1/member/login` | `login` | 否 | ✅ MEMBER_LOGIN |
| 3 | POST `/api/v1/member/logout` | `logout` | 是（Bearer） | - |
| 4 | POST `/api/v1/member/profile/get` | `getProfile` | 是 | - |
| 5 | POST `/api/v1/member/profile/update` | `updateProfile` | 是 | ✅ MEMBER_PROFILE_UPDATE |

層級：Controller → Service interface → ServiceImpl `@Transactional` → MemberMapper / UserPreferenceMapper（MyBatis） → UserPO / UserPreferencePO。
DTO 全用 Java 21 record（含 Bean Validation 標註）。
Convertor 使用 MapStruct（`componentModel = "spring"`）。

### 1.5 Flyway Migrations

| 版本 | 檔名 | 內容 |
|------|------|------|
| V1.0.0 | `create_member_tables.sql` | `users` + `user_preferences`（含 FK + UK + 索引） |
| V1.0.1 | `create_audit_log.sql` | `audit_logs`（含 user_id / action / trace_id 三組索引） |

PostgreSQL 16 語法；TIMESTAMP WITHOUT TIME ZONE；主鍵 VARCHAR(36)。

### 1.6 Profile

| Profile | 用途 | 配置來源 |
|---------|------|----------|
| `local`（預設） | 本機 docker-compose；明碼 | `application-local.yml` |
| `dev` | CI/CD；環境變數 | `application-dev.yml` |
| `prod` | 生產；環境變數；無 baseline-on-migrate | `application-prod.yml` |

### 1.7 單元測試

| Test class | case 數 | 範圍 |
|------------|---------|------|
| `BigDecimalUtilsTest` | 5 | safeAdd / Multiply / Divide / scale / nullToZero |
| `ApiResponseTest` | 3 | success / fail(ErrorCode) / ErrorCode.fromCode |
| `MemberServiceImplTest` | 4 | register 成功 / email 重複 / login 成功 / login 密碼錯 |

合計 12 個 case（4 個必要 + 8 個額外覆蓋）。

---

## 2. 已知技術債（Wave 1 暫時繞過）

| # | 項目 | 原因 | 預計處理 wave |
|---|------|------|--------------|
| TD-1 | JWT 黑名單 & Refresh Token | Wave 1 範圍只要 access token + logout stub | Wave 2 |
| TD-2 | Spring Security Filter Chain | Controller 直接解 token；Wave 2 改正規 SecurityFilterChain + JwtAuthenticationFilter | Wave 2 |
| TD-3 | Email 驗證流程 | register 暫時直接設 status=ACTIVE | Wave 2 |
| TD-4 | 修改密碼 / 帳號註銷 / Refresh / 重發驗證信 | 不在 Wave 1 範圍 | Wave 2 |
| TD-5 | ArchUnit 模組依賴規則測試 | Wave 1 module 邊界尚少；待 Wave 2 業務模組逐步進場時補上 | Wave 2 |
| TD-6 | OWASP Dependency Check Maven plugin | 等 Wave 1 通過初次 build 後再加入 verify phase | Wave 2 |
| TD-7 | MapStruct lombok-binding | 已在 parent pluginManagement 設定，但子模組需自行於 build/plugins 啟用（Wave 1 stock-member 編譯如出狀況需個別補） | Wave 2 確認 |
| TD-8 | Telegram chat_id 綁定 / FCM token 上傳 API | M-MEMBER profile 已有欄位但無 API；對應 D4 推播設計 | Wave 3 隨 M-NOTIFY 一起 |
| TD-9 | i18n 訊息（合規文案） | ErrorCode 訊息硬編碼繁中；無多語需求前不抽出 | Wave 4 上線前 |
| TD-10 | 整合測試（Testcontainers PG 起 Mapper SQL 驗證） | 需待 Mapper SQL 變多再批次驗 | Wave 2 |

---

## 3. Wave 2 注意事項

### 3.1 必須先完成

1. **Spring Security 正規化** — 接替 Controller 自解 token 的暫時做法
   - 建立 `SecurityFilterChain`、`JwtAuthenticationFilter`、`UserPrincipal`
   - `@PreAuthorize` 控制 endpoint
   - 把 currentUserId 從 SecurityContext 取得（非從 header 解）
   - 同時把 MDC `userId` 注入提到 filter，AuditAspect 才會帶到正確 userId
2. **完成 M-MEMBER 剩餘 5 支 API**
   - verify-email、resend-verification、refresh-token、password/change、account/close
   - 需新增 `EMAIL_VERIFY_TOKEN` 表（Flyway V1.0.2）
3. **ArchUnit 規則** — 在 stock-boot test 下加：
   - `business module 不可橫向依賴除 SRS §8.2 宣告外的其他 business module`
   - `controller package 內類別必須以 Controller 結尾`
   - `service.impl 不可被 controller 直接 import`

### 3.2 開發順序建議（與 Squad 對齊）

| Squad | Wave 2 接力 |
|-------|------------|
| Squad A（會員與互動） | 完成 M-MEMBER 剩餘 5 支 + 啟動 M-WATCH |
| Squad B（資料層） | 啟動 M-QUOTE + M-CHIP 的 stock-infrastructure client（TWSEClient / OTCClient） |
| Squad C（指標與評分） | 等 Squad B 提供 QUOTE_DAILY 後啟動 M-TECH calculator |

### 3.3 共享 PO 變更協議

依 ProjectArch §1.5「Squad 共享 stock-domain 是衝突熱區」，**任何 PO/Enum 變更必須走 PR review by Preston**，避免 Squad 之間合併衝突。

### 3.4 風險預警

- **PostgreSQL stringtype=unspecified**：JDBC URL 已加，避免 enum 字串轉型問題；如後續欄位用到 PG enum type，請務必複測。
- **Audit 非同步**：`AuditEventPublisherImpl` 用 `@Async("auditExecutor")`，當 audit_logs 寫入失敗只 log 不擲出。**Wave 4 上線前需設置 monitoring alert**（log level=ERROR + log keyword=`Audit log persist failed`）。
- **JWT secret**：local profile 已設明碼，dev/prod 透過 `STOCK_JWT_SECRET` 注入；長度需 ≥ 32 字元（HS256 要求）。

---

## 4. 與 Linus 評估的衝突點

**無新衝突**。本次實作對 Linus 評估的依賴版本完全沿用：
- Spring Boot 3.3.13、MyBatis Spring Boot 3.0.5、PostgreSQL 42.7.5、Flyway 10.20.1、JJWT 0.12.6、MapStruct 1.6.3、Lombok 1.18.34（隨 Spring Boot 帶入）。
- 額外引入：`at.favre.lib:bcrypt:0.10.2`（避免 Wave 1 強耦合 Spring Security 全套）— 已在 Library 評估 §1.10 隱性涵蓋（規範允許 BCrypt）。

**對 Preston 設計的微調**：
- ProjectArch 原文寫 12 module，但 1.2 圖示為 14 個（含 parent + 10 業務 + 3 base + 1 boot）。本次實作以**圖為準**，14 個 module + parent。
- USER_INFO 表名改為 `users`（Preston ER 圖名）— 同時對應 Java 規範「避免 RDMS 保留字」（`USER` 是 PG 保留字、`USERS` 加 s 即可避雷）。

---

## 5. 對 Jamie 的回報摘要

詳見 Bash 訊息中 200 字摘要。

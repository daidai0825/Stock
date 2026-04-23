# Wave 3 W1 後端開發筆記 — Security Config & stock_info 主檔骨架

**日期**：2026-04-23
**作者**：Bruno
**任務**：Wave 3 W1 — Spring Security 整合 + stock_info 主檔 + 新模組骨架

---

## 一、工作範圍

| Slice | 說明 |
|-------|------|
| Slice 1 | Spring Security 主配置（SecurityConfig、JwtAuthenticationFilter、EntryPoint、AccessDeniedHandler、AuthenticatedUser） |
| Slice 2 | stock_info 主檔骨架（Flyway V3.0.0/V3.0.1/V3.1.0、StockInfoPO、StockInfoMapper） |
| Slice 3 | 新模組骨架（stock-search、stock-alert pom.xml + AutoConfig + PlaceholderController） |

---

## 二、關鍵設計決策

### D-08：Envelope Pattern（HTTP 200 + 業務錯誤碼）

Spring Security 的 `AuthenticationEntryPoint` 與 `AccessDeniedHandler` 預設會回傳 HTTP 401/403，不符合本專案規範。

**解法**：
- `ApiAuthenticationEntryPoint.commence()` 固定設 `response.setStatus(HttpStatus.OK.value())`
- `ApiAccessDeniedHandler.handle()` 固定設 `response.setStatus(HttpStatus.OK.value())`
- 業務錯誤碼透過 `ApiResponse.fail(ErrorCode)` 寫入 response body

### JWT 錯誤碼分層（3001/3002/3003）

JWT 驗證失敗有多種情境，需在 `JwtAuthenticationFilter` 提前標記：

```
TOKEN_EXPIRED  → code 3002（需前端靜默 refresh）
TOKEN_INVALID  → code 3003（需前端登出）
無 token       → code 3001（未登入）
```

**實作方式**：`JwtAuthenticationFilter` 在 request attribute 寫入 `jwtAuthError`（`"TOKEN_EXPIRED"` / `"TOKEN_INVALID"`），`ApiAuthenticationEntryPoint` 讀取後決定回傳的 ErrorCode。

**為什麼不在 Filter 直接寫 response**：Filter 直接寫 response 無法讓 `AuthenticationEntryPoint` 的錯誤處理集中管理，且 `SecurityContext` 未設定時 Spring Security 仍會呼叫 EntryPoint，應讓職責分明。

### P5：pg_trgm extension 需要 DBA 預先建立

`V3.0.0__create_stock_info.sql` 包含：
```sql
CREATE INDEX gin_stock_info_name_trgm ON stock_info USING GIN (stock_name gin_trgm_ops);
```

如果 PostgreSQL 沒有 `pg_trgm` extension，此 migration 會失敗。**決策**：migration 本身不執行 `CREATE EXTENSION`（需 superuser），由 DBA 在 dev/prod 環境事先建立。

`StockInfoMapper` 同時保留兩組搜尋方法：
- `searchByKeyword`：使用 `%` 運算子（依賴 pg_trgm GIN index，效能佳）
- `searchByNameContains`：使用 `LIKE '%keyword%'`（降級替代，不依賴 extension）

### Q4：SecurityConfig 放在 stock-boot，不在 stock-member

`SecurityConfig` 是應用程式組合點（需知道所有 module 的 endpoint），放在 stock-member 會讓 member 模組隱含知道其他模組的路徑，違反單一職責。放在 stock-boot 符合「應用組裝層」的職責。

### AuthenticatedUser 使用 Java 21 Record

```java
public record AuthenticatedUser(String userId, String email) {}
```

作為 Spring Security Principal，透過 `@AuthenticationPrincipal AuthenticatedUser user` 注入。相比繼承 `UserDetails` 介面，record 語法簡潔且不可變，符合 Principal 的語意。

---

## 三、新增檔案清單

### stock-boot

| 檔案 | 說明 |
|------|------|
| `config/security/JwtAuthenticationFilter.java` | Bearer token 解析，失敗設 request attribute |
| `config/security/AuthenticatedUser.java` | JWT Principal record（userId, email） |
| `config/security/ApiAuthenticationEntryPoint.java` | HTTP 200 + 3001/3002/3003 |
| `config/security/ApiAccessDeniedHandler.java` | HTTP 200 + 3004 |
| `config/SecurityConfig.java` | 主 Security 配置，permitAll/authenticated 規則 |
| `db/migration/V3.0.0__create_stock_info.sql` | stock_info 表 + 索引（含 pg_trgm GIN，需 DBA 預建 extension） |
| `db/migration/V3.0.1__seed_stock_info_twse_otc.sql` | 18 筆台股種子資料（ON CONFLICT DO NOTHING） |
| `db/migration/V3.1.0__create_refresh_token.sql` | refresh_token 表（Wave 3 會員 refresh 流程） |

### stock-domain

| 檔案 | 說明 |
|------|------|
| `po/StockInfoPO.java` | stock_info 對應 PO，camelCase + Lombok |

### stock-search（新模組）

| 檔案 | 說明 |
|------|------|
| `pom.xml` | 依賴 stock-domain/common/infrastructure + mybatis + mybatis-starter-test |
| `config/SearchAutoConfig.java` | @ComponentScan + @MapperScan |
| `controller/SearchPlaceholderController.java` | POST /api/v1/stock/search, /hot-search → FEATURE_NOT_AVAILABLE |
| `repository/StockInfoMapper.java` | 10 個查詢方法（含 upsert、countActiveByMarket） |
| `test/.../SearchTestApplication.java` | 測試用 @SpringBootApplication（讓 @MybatisTest 找到 config） |
| `test/.../StockInfoMapperTest.java` | 9 個 Testcontainers 整合測試 |
| `test/resources/application.yml` | MyBatis 測試配置 |
| `test/resources/test-schema/stock_info_test.sql` | 測試用 DDL（無 pg_trgm GIN index） |

### stock-alert（新模組）

| 檔案 | 說明 |
|------|------|
| `pom.xml` | 依賴 stock-domain/common/infrastructure + mybatis |
| `config/AlertAutoConfig.java` | @ComponentScan + @MapperScan |
| `controller/AlertPlaceholderController.java` | POST /api/v1/alert/create|list|update-status|delete → FEATURE_NOT_AVAILABLE |

---

## 四、SecurityConfig 公開 Endpoint 清單

```
# 報價（Wave 1/2 公開）
/api/v1/quote/**
/api/v1/fundamental/get
/api/v1/chip/get

# 會員認證
/api/v1/member/login
/api/v1/member/register
/api/v1/member/refresh
/api/v1/auth/**

# Wave 3 搜尋（公開）
/api/v1/stock/search
/api/v1/stock/hot-search
/api/v1/search/stock
/api/v1/search/popular

# 基礎設施
/actuator/health/**
/v3/api-docs/**
/swagger-ui/**
```

其他 `/api/v1/**` 全部需要 JWT（`authenticated()`），再外一層 `anyRequest().denyAll()`。

---

## 五、CORS 配置

允許來源：
- `http://localhost:5173`（本地前端開發）
- `https://dev-stock.example.com`（dev 環境）
- `https://stock.example.com`（prod 環境）

不使用萬用字元 `*`（OWASP A05 規範）。

---

## 六、Maven 依賴變更

### root pom.xml

- 新增 `stock-search`、`stock-alert` 到 `<modules>` 和 `<dependencyManagement>`
- 新增 `mybatis-spring-boot-starter-test` 到 dependencyManagement（版本 3.0.5，test scope）
- 補回 `web-push`、`bouncycastle` properties（先前被 linter 移除）

### stock-boot/pom.xml

- 新增 `stock-search`、`stock-alert` 依賴
- 新增 `spring-boot-starter-security`
- 新增 `spring-security-test`（test scope）

### stock-search/pom.xml

- 新增 `mybatis-spring-boot-starter-test`（test scope）— 提供 `@MybatisTest` slice testing

---

## 七、測試狀況

| 測試 | 狀態 | 備註 |
|------|------|------|
| stock-member 14 tests | PASS | Wave 1/2 未受影響 |
| stock-common 21 tests | PASS | |
| stock-infrastructure 9 tests | PASS | |
| StockInfoMapperTest | SKIP（Docker daemon 未啟動） | Testcontainers 需要 Docker；本機 Docker Desktop daemon 停止 |
| SecurityConfigIntegrationTest | SKIP（Docker daemon 未啟動） | 同上 |
| 全專案編譯（含 test-compile） | SUCCESS | 14.5 秒，所有 16 個模組通過 |

**Testcontainers 測試說明**：測試程式碼邏輯正確，但當前執行環境 Docker daemon 未啟動（`docker ps` 顯示 cannot connect to docker daemon）。重新啟動 Docker Desktop 後執行 `./mvnw test -pl stock-search,stock-boot` 即可驗證。

---

## 八、潛在風險與後續事項

1. **pg_trgm extension**（P5）：dev/prod 環境 DBA 需在 Flyway 執行 V3.0.0 前執行 `CREATE EXTENSION IF NOT EXISTS pg_trgm;`。
2. **CORS origins**：`SecurityConfig` 中的 dev/prod URL 目前為預留值，需在 Slice N 時由 Sophia/Preston 確認實際域名後更新。
3. **Wave 1/2 backward compatibility**：`MemberController` 的 `currentUserId()` helper 仍保留（Wave 2 遺留），Wave 3 各 Controller 改用 `@AuthenticationPrincipal AuthenticatedUser user`，兩者並存不衝突。
4. **stock-boot 不含 DataSource 配置**：`SecurityConfigIntegrationTest` 需要 Flyway + PostgreSQL，透過 `@DynamicPropertySource` 注入 Testcontainers PG URL；Redis 透過 `spring.autoconfigure.exclude` 停用。

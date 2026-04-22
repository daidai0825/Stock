# Code Review 報告 — Wave 1（後端骨架）

## 基本資訊

| 項目 | 內容 |
|------|------|
| 審查者 | Brian（Senior Backend Reviewer） |
| 審查日期 | 2026-04-22 (GMT+8) |
| 審查範圍 | Stage 4 / Wave 1 後端骨架 |
| 開發者 | Bruno |
| 變更分支 | feature/wave1-skeleton（尚未開 PR） |
| 變更檔案數 | 約 60 個（14 module + parent + Flyway + docker-compose + 3 test class） |
| 受審目錄 | `/usr/local/dale/daidai0825/Stock/backend/` |
| 開發筆記 | `docs/05_development/backend/20260422_wave1_skeleton.md` |
| Review 輪次 | #1 |

## 摘要

| 嚴重度 | 數量 |
|--------|------|
| 🔴 Blocker | 2 |
| 🟡 Major | 6 |
| 🟢 Minor | 5 |
| ✓ 良好實踐 | 6 |

**最終判定**：🟡 GO-WITH-FIXES（2 個 Blocker 必修，但範圍小、修復可在數小時內完成；無架構級退回）

---

## 🔴 Blocker（必須修復）

### #1 `MemberMapper.insert()` 誤用 `@Select` 註解寫 INSERT

| 項目 | 內容 |
|------|------|
| 檔案位置 | `backend/stock-member/src/main/java/tw/com/stockplatform/member/repository/MemberMapper.java:52-61` |
| 嚴重度 | 🔴 嚴重 |
| 類別 | 邏輯錯誤 / 框架誤用 |

**問題描述**：

```java
@Select("""
    INSERT INTO users
        (user_id, email, ...)
    VALUES
        (#{userId}, #{email}, ...)
    RETURNING user_id
    """)
String insert(UserPO po);
```

此處為寫入操作，卻使用 `@Select` 註解。MyBatis 透過 `@Select` 走 `executeQuery`（非 `executeUpdate`）路徑，雖然 PostgreSQL 的 `RETURNING` 子句語法層面可以執行，但會出現以下問題：

1. **語意錯誤**：MyBatis statement type 將被視為 `SELECT`，在某些版本 / executor type（如 BATCH）下行為不一致，可能導致 SQL 不被列入 batch flush。
2. **與 `@Transactional` 行為相容性風險**：Spring `@Transactional` 對「query-only」statement 在某些 propagation 配置下不會強制取得寫入交易。
3. **單元測試完全 mock 掉 mapper 故無法暴露此問題**：實機跑 PostgreSQL 才會發現異常。
4. **同一檔案的 `findById`、`findByEmail` 也會被覆蓋取值差異**（因 statement type 影響 cache、prepared 行為）。

**建議修正**：

```java
@Insert("""
    INSERT INTO users
        (user_id, email, password_hash, display_name, status,
         email_verified_at, created_at, updated_at, deleted_at)
    VALUES
        (#{userId}, #{email}, #{passwordHash}, #{displayName}, #{status},
         #{emailVerifiedAt}, #{createdAt}, #{updatedAt}, #{deletedAt})
    """)
int insert(UserPO po);
```

回傳改為 `int`（受影響筆數）。`RETURNING` 子句僅在「需要 DB 端產 ID」時才需保留，本案 `userId` 已由 Java 端 `UUID.randomUUID()` 產生，無此需求。

**參考**：
- MyBatis 3.5 官方文件 — Insert/Update/Delete 必須使用對應 annotation
- 同檔案 `UserPreferenceMapper.insert()`（line 37）正確使用 `@Insert`，可見 Bruno 知道正確寫法，此處純屬複製/編輯失誤

---

### #2 `JwtTokenProvider.parse()` 例外類型未區分，`MemberController.currentUserId` 一律回 `TOKEN_INVALID`，導致過期 token 無法回 `TOKEN_EXPIRED(3002)`

| 項目 | 內容 |
|------|------|
| 檔案位置 | `backend/stock-member/src/main/java/tw/com/stockplatform/member/controller/MemberController.java:77-81` 與 `JwtTokenProvider.java:55-61` |
| 嚴重度 | 🔴 嚴重 |
| 類別 | API 契約 / 業務碼分段違規 |

**問題描述**：

`MemberController.currentUserId()`：

```java
try {
    return jwtTokenProvider.parse(header.substring(BEARER_PREFIX.length())).getSubject();
} catch (Exception ex) {
    throw new BusinessException(ErrorCode.TOKEN_INVALID);
}
```

`jjwt 0.12.x` 對「過期 token」會擲 `ExpiredJwtException`，對「簽章不符 / 格式錯誤」擲 `JwtException` / `IllegalArgumentException`。目前一律 catch `Exception` 並回 `TOKEN_INVALID(3003)`，造成：

1. **違反 SRS §9 業務碼分段**：`ErrorCode.TOKEN_EXPIRED(3002)` 永遠不會被觸發。
2. **前端無法判斷是否該觸發 refresh token 流程**（Wave 2 啟用時就會踩雷）。
3. **跨團隊契約風險**：Felix（前端 Reviewer 視角）依 SRS 寫的 401/3002 處理路徑將沒有對應路徑進入。

**建議修正**：

```java
private String currentUserId(HttpServletRequest request) {
    String header = request.getHeader(AUTH_HEADER);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    String token = header.substring(BEARER_PREFIX.length());
    try {
        return jwtTokenProvider.parse(token).getSubject();
    } catch (io.jsonwebtoken.ExpiredJwtException ex) {
        throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }
}
```

或更佳：在 `JwtTokenProvider` 內部攔好 jjwt 例外，往外擲自訂 `TokenExpiredException` / `TokenInvalidException`（業務語意，避免 controller 直接耦合 jjwt 套件）— 此屬 Wave 2 重構，Wave 1 至少需修上述「兩個 catch 分支」。

**註**：此問題在 `currentUserId` 被 4 支 API（logout / profile/get / profile/update + Wave 2 將擴增的 5 支）共用，影響面廣。

---

## 🟡 Major（強烈建議修復）

### #3 `UserPreferencePO` 在 `getProfile` 找不到時用 `Optional.orElse(builder().build())` 隱藏資料完整性破口

| 項目 | 內容 |
|------|------|
| 檔案位置 | `MemberServiceImpl.java:125-130` |
| 嚴重度 | 🟡 中等 |
| 類別 | 資料完整性 / 例外處理 |

**問題描述**：

```java
UserPreferencePO pref = userPreferenceMapper.findByUserId(userId)
    .orElse(UserPreferencePO.builder()
        .notifyEmailEnabled("Y")
        .notifyWebEnabled("Y")
        .notifyTelegramEnabled("N")
        .build());
```

`register` 流程必定建立 `user_preferences`（且 FK CASCADE），所以查不到代表資料**已被破壞**或**Wave 2 新流程漏建**。此處用「預設值靜默兼容」會讓資料異常被掩蓋。

但同一個檔案 `updateProfile`（line 158）卻會擲 `USER_NOT_FOUND`，行為**前後不一致**。

**建議修正**：

```java
UserPreferencePO pref = userPreferenceMapper.findByUserId(userId)
    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
```

保持 `getProfile` 與 `updateProfile` 一致；若未來真有「無偏好」情境，需明確新增 ErrorCode 與業務文件描述，而非靜默回填。

---

### #4 `AuditAspect.userId` 來自 MDC 但 Wave 1 全程未設定，`MEMBER_REGISTER` / `MEMBER_LOGIN` audit 永遠寫入 `userId = null`

| 項目 | 內容 |
|------|------|
| 檔案位置 | `AuditAspect.java:43`、`MemberController.java`（無 MDC.put userId） |
| 嚴重度 | 🟡 中等 |
| 類別 | A4 拍板履行不到位 |

**問題描述**：

A4（Audit Log v1）拍板要求記錄使用者操作行為。但：

1. `MEMBER_REGISTER`：行為開始時根本還沒 user，audit 寫 null 屬合理 — 但應在 `register` **成功後**將 `MDC.put("userId", po.getUserId())` 補回，讓 `@AfterReturning` 切面有值。
2. `MEMBER_LOGIN`：登入成功才取得 userId，目前一樣 audit `userId = null`。
3. `MEMBER_PROFILE_UPDATE`：使用者已登入，但 `currentUserId()` 解出的 userId 並未塞進 MDC，所以這條 audit **也是 null**。

結果：所有 audit_logs 的 `user_id` 欄位都是 NULL，A4 等於沒落地。

**建議修正（最小幅度）**：

A. 在 `MemberServiceImpl.login` / `register` 成功路徑：

```java
String token = jwtTokenProvider.createAccessToken(po.getUserId(), po.getEmail());
MDC.put("userId", po.getUserId());  // 讓 @AfterReturning 抓到
return new LoginResponse(...);
```

B. 在 `MemberController.currentUserId()` 解 token 後立刻 `MDC.put("userId", subject)`，並在 `TraceIdFilter` 的 finally 加 `MDC.remove("userId")`（避免 thread reuse 污染）。

C. Wave 2 改用 SecurityContext 後，將 MDC 注入提到 `JwtAuthenticationFilter`，與 TD-2 同步處理。

Bruno 自陳的 TD-2 雖提到 SecurityContext，但**沒提這個 audit-userId 漏洞**；屬「自陳 TD 缺漏」，需明確登錄。

---

### #5 `register` 成功路徑在 audit log 中 detail 直接帶 `SUCCESS`，無寫入 userId，事後無法稽核「哪個 email 被註冊」

| 項目 | 內容 |
|------|------|
| 檔案位置 | `AuditAspect.java:31`、`audit_logs` 欄位設計 |
| 嚴重度 | 🟡 中等 |
| 類別 | 合規（個資稽核） |

**問題描述**：

A4 audit 主要用途之一是「個資合規追蹤」（SRS 應有要求）。現況 audit 只記 `action=MEMBER_REGISTER, target=USER, detail=SUCCESS`，無 email、無 userId（同 #4），等於「有人註冊了一個帳號」但**完全無法稽核是誰**。

**建議修正**：

`AuditAspect` 對 `MEMBER_REGISTER`、`MEMBER_LOGIN` 兩個動作，從 `joinPoint.getArgs()` 萃取 email（屬於 PII，**遮罩後**寫入 `detail`），例如：

```java
detail = "SUCCESS, email=t***@example.com"
```

或在 Service 內手動發 audit event（不走 aspect），帶上更豐富的 detail 欄位。

**注意**：絕對禁止寫入密碼、token；email 也應遮罩（前 1 字 + `***` + `@domain`）— `MaskUtils` 應於 stock-common 補上（目前缺）。

---

### #6 `application-local.yml` JWT secret 為固定明碼且註解寫「please change」— 沒人會改

| 項目 | 內容 |
|------|------|
| 檔案位置 | `backend/stock-boot/src/main/resources/application-local.yml:33` |
| 嚴重度 | 🟡 中等 |
| 類別 | 安全 / 環境配置 |

**問題描述**：

```yaml
secret: local-dev-secret-please-change-this-must-be-at-least-32-chars
```

依 `environment.md` local 可明碼，**規範允許**。但有兩個風險：

1. 此 secret 字面值若被工程師複製到 dev/prod 環境（人為錯誤），CI 沒擋。
2. 該 secret **一字不差地會出現在 Wave 4 上線前的程式碼掃描**（Trivy / GitGuardian）— 即便 local-only，也會誤觸告警。

**建議修正**：

A. 將 secret 改為**每次啟動時隨機生成的環境變數**（`local` profile fallback 到 `${STOCK_JWT_SECRET:auto-generated-uuid-32-chars}`）— 但這會破壞 IDE 直接 Run。

B. 折衷：保留明碼但加上 `application-local.yml` 註解「**禁止複製到任何 dev/prod yml**」並在 CI 加 grep guard：若 dev/prod yml 出現 `local-dev-secret` 字串就 fail build。

C. 配上 `.env.example` 給工程師參考。

選 B + C 即可，工作量小但有效。

---

### #7 `LoginResponse` 內嵌 `MemberDTO`，但 `LoginResponse` 應只回 token；個資由獨立 `/profile/get` 取得，避免 token endpoint 洩漏個資範圍擴大

| 項目 | 內容 |
|------|------|
| 檔案位置 | `LoginResponse.java`、`MemberServiceImpl.login` |
| 嚴重度 | 🟡 中等 |
| 類別 | API 設計 / 安全（最小揭露） |

**問題描述**：

```java
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    MemberDTO member       // <- 此欄位
) {}
```

`LoginResponse.member` 暴露了 `userId / email / displayName / status / createdAt`。Login endpoint 走「無認證」入口，回應若被中介攔截（HTTP/HTTPS 中斷、proxy log）會洩漏更多 PII。

**建議修正**：

依 OWASP A02:2021「最小揭露原則」：login 只回 token + expires + tokenType，前端拿到 token 後再呼叫 `/profile/get`。

若 PM 堅持為了減少前端 call 次數合併在 login，至少把 `MemberDTO.email` 移除（前端可由 token claim 自行解出）。

**跨團隊提醒**：此議題**請 Felix 確認**前端是否依賴 `LoginResponse.member.*`；若無依賴，可立即移除。

---

### #8 `MemberServiceImpl.updateProfile` 在 service 內手動更新 `po.setDisplayName / setUpdatedAt`，但更新後並未呼叫 `findById` 重新讀取，DTO 回傳值與 DB 不保證一致

| 項目 | 內容 |
|------|------|
| 檔案位置 | `MemberServiceImpl.java:142-167` |
| 嚴重度 | 🟡 中等 |
| 類別 | 資料一致性 |

**問題描述**：

```java
if (request.displayName() != null) {
    memberMapper.updateDisplayName(userId, request.displayName(), now);
    po.setDisplayName(request.displayName());   // <- 手動同步
    po.setUpdatedAt(now);
}
// ...
return memberConvertor.toDTO(po);
```

問題：

1. 手動同步 `po` 易遺漏欄位（例如未來新增欄位忘記同步）。
2. 若 update SQL 失敗（影響筆數 0、被觸發器修改），回傳的 DTO 仍顯示「成功更新後」的值，造成前端誤判。
3. `memberMapper.updateDisplayName` 回傳 `int` 但 service 完全沒檢查筆數（`if (rows == 0) throw USER_NOT_FOUND`）。

**建議修正**：

```java
int rows = memberMapper.updateDisplayName(userId, request.displayName(), now);
if (rows == 0) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}
// 重新讀，避免狀態漂移
UserPO refreshed = memberMapper.findById(userId)
    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
return memberConvertor.toDTO(refreshed);
```

---

## 🟢 Minor（可選修復）

### #9 `ErrorCode.fromCode` 用 Stream 處理 41 個 enum value，違反 stream 使用準則「<10 元素且簡單操作」

| 項目 | 內容 |
|------|------|
| 檔案位置 | `ErrorCode.java:85-89` |
| 嚴重度 | 🟢 輕微 |
| 類別 | 規範違反（弱） |

41 個 enum 不算少，且 `fromCode` 是熱路徑（GlobalExceptionHandler 可能反查），改為 `Map<Integer, ErrorCode>` 預建 lookup 更佳：

```java
private static final Map<Integer, ErrorCode> BY_CODE =
    Stream.of(values()).collect(Collectors.toMap(ErrorCode::getCode, Function.identity()));

public static Optional<ErrorCode> fromCode(int code) {
    return Optional.ofNullable(BY_CODE.get(code));
}
```

`UserStatus.fromName` 同理（4 個元素無所謂，可保留）。

---

### #10 `TimeUtils.toIso8601(null)` 回 `null`，違反 NPE-Free 設計可改為 Optional

| 項目 | 內容 |
|------|------|
| 檔案位置 | `TimeUtils.java:36-41` |
| 嚴重度 | 🟢 輕微 |

依 `java-development.md`，util 工具中的「轉換 / 反查」可使用 Optional。建議：

```java
public static Optional<String> toIso8601(LocalDateTime dateTime) {
    return Optional.ofNullable(dateTime).map(d -> d.atZone(TAIPEI).format(ISO_OFFSET));
}
```

---

### #11 `BigDecimalUtils.fromDouble` 註解寫「僅用於資料邊界轉換」，但無編譯期保護

可加 `@Deprecated` + `since` 或抽到 `BoundaryConvertUtils` 獨立類，避免被誤用。

---

### #12 placeholder Controller 的 `_status` endpoint 設計分歧 — 應回 `200/0`「模組未開」而非 `200/9001`

| 項目 | 內容 |
|------|------|
| 檔案位置 | 9 個 placeholder controller |
| 嚴重度 | 🟢 輕微 |

依 `api-design.md`，`9001 SYSTEM_BUSY` 語意是「系統繁忙請稍後再試」，與「模組尚未實作」不同。建議新增 `9003 MODULE_NOT_AVAILABLE` 錯誤碼，或一律改回 `0/success` + `data="not implemented"`，前者較符合語意。

`code` 改為 `9003`、訊息「Wave 1 placeholder：本模組將於 Wave 2+ 上線」。

---

### #13 docker-compose.yml 缺少 `version` 與 `networks` 顯式宣告，依賴 Docker Compose 預設

不影響運作但顯式較好維運。

---

## ✓ 良好實踐（鼓勵）

| # | 實踐 | 位置 |
|---|------|------|
| 1 | 全 5 支 API 一律回 `ApiResponse<T>`，HTTP 200 + 業務碼，**Envelope 落實到位** | `MemberController` |
| 2 | DTO 全用 Java 21 record，符合 `java-spring.md` 鼓勵 | `dto/request/*.java`、`dto/response/*.java` |
| 3 | BCrypt cost = 12，符合 `security-owasp.md` A02 規範（≥12） | `PasswordEncoder.java:14` |
| 4 | MyBatis 一律 `#{}`，無 `${}` 出現 — 0 個 SQL Injection 風險 | `MemberMapper`、`UserPreferenceMapper`、`AuditLogMapper` |
| 5 | `@Transactional(readOnly = true)` 用在 `getProfile`，效能優化正確 | `MemberServiceImpl.java:120` |
| 6 | TraceIdFilter `Order.HIGHEST_PRECEDENCE` + finally 清 MDC，符合最佳實踐 | `TraceIdFilter.java` |

---

## 整體評價

| 維度 | 評分 | 說明 |
|------|------|------|
| 架構符合度 | 8/10 | 14 module + parent 與 ProjectArch 一致，分層清楚；少數 audit/MDC 細節未到位 |
| 程式碼品質 | 7/10 | Java 21 record、MapStruct、Optional 使用得當；2 處 framework 誤用 |
| 安全性 | 7/10 | BCrypt 與 `#{}` 落實良好；JWT 例外分流不全、login 回應揭露過多、無 PII 遮罩 |
| API 契約符合度 | 8/10 | Envelope 一致；錯誤碼分段對齊；TOKEN_EXPIRED 路徑斷裂為扣分主因 |
| 測試覆蓋率（粗估） | 6/10 | 12 case 覆蓋核心 happy path + 2 個 fail case；無 controller test、無 integration test、無 audit aspect test。粗估行覆蓋率 ~45%（未達 80% Quality Gate） |
| 環境配置 | 8/10 | local 明碼 + dev/prod 外部變數規範到位；缺 secret 隔離防線 |
| 文件完整度 | 9/10 | Bruno 開發筆記 + 10 項 TD 自陳，誠實透明 |
| 依賴管理 | 9/10 | 與 Linus 評估一致，僅多引 `bcrypt 0.10.2`（已說明） |
| **總評** | **7.5/10** | **可進入 Wave 2，2 處 Blocker 需於 Wave 2 前 1-2 天內修完** |

---

## 對 Bruno 自陳 10 項 TD 的逐項評估

| TD # | 項目 | Brian 評估 | 處理時程 |
|------|------|-----------|----------|
| TD-1 | JWT 黑名單 & Refresh Token | **合理延後**。Wave 1 logout 屬 stub 可接受，但需在 release notes 註明「目前 logout 不真正撤銷 token」 | Wave 2 |
| TD-2 | Spring Security 正規化 | **合理延後**，但**請補上 audit-userId MDC 注入**（見 #4） | Wave 2 |
| TD-3 | Email 驗證流程 | **合理延後**。但 `users.status DEFAULT 'UNVERIFIED'` 與 service 寫死 `ACTIVE` 不一致（DEFAULT 形同虛設），請文件加註 | Wave 2 |
| TD-4 | 修改密碼 / 註銷 / Refresh / 重發驗證 | **合理延後**，已在 Bruno 筆記 §3.1 列入 Wave 2 必做 | Wave 2 |
| TD-5 | ArchUnit 規則 | **應提前到 Wave 2 第一週**。Bruno 自己也說「業務模組逐步進場時補」— 一旦 Squad 並行開發，違規會大量出現難收拾 | Wave 2 第 1 週 |
| TD-6 | OWASP Dependency Check Maven plugin | **合理延後**。`dependency-check.xml` 已就位，等 Wave 2 build 通過後即接 | Wave 2 |
| TD-7 | MapStruct lombok-binding 子模組啟用 | **應提前驗證**。Wave 1 編譯若有警告或 NPE 再修就太遲，建議 Bruno 立刻在 stock-member 跑 `mvn clean compile` 確認 | 立即驗證 |
| TD-8 | Telegram chat_id / FCM token 上傳 API | **合理延後** | Wave 3 |
| TD-9 | i18n 訊息 | **合理延後**。Wave 4 上線前處理 | Wave 4 |
| TD-10 | Testcontainers 整合測試 | **應提前到 Wave 2 第一週**。MyBatis SQL 是運行期才驗證的，現況單元測試 100% mock，#1 Blocker（@Select 寫 INSERT）就是「沒整合測試」漏掉的 — 建議至少把 `MemberMapper` 三支方法用 Testcontainers 跑一次 | Wave 2 第 1 週 |

**Bruno 自陳 TD 缺漏項（Brian 補充）**：

| 補充 # | 項目 | 嚴重度 | 處理時程 |
|--------|------|--------|----------|
| TD-A | audit_logs 的 user_id 欄位永遠寫 null（見 #4） | 🟡 | Wave 2 |
| TD-B | login 回應內嵌個資洩漏面擴大（見 #7） | 🟡 | 與 Felix 對齊後決定 |
| TD-C | PII 遮罩工具（MaskUtils）尚未建立 | 🟡 | Wave 2（與 audit detail 一起） |
| TD-D | controller 整合測試（@WebMvcTest）零覆蓋 | 🟡 | Wave 2 |
| TD-E | SecurityHeadersConfig 缺（HSTS / X-Frame-Options / CSP 全無） | 🟡 | Wave 2（接 Spring Security 時順便） |

---

## 對 Felix 的跨團隊提醒（API 契約議題）

請 **Jamie** 轉達 **Felix**（前端 Reviewer）：

1. **`/api/v1/member/login` 回應 `LoginResponse.member` 欄位是否被前端依賴？**
   - 若否：建議移除（見 Major #7），降低 PII 揭露面
   - 若是：保留但請 Felix 在前端 review 時確保 `LoginResponse.member.email` 不會被存入 localStorage / sessionStorage 明碼
2. **`TOKEN_EXPIRED(3002)` vs `TOKEN_INVALID(3003)` 區分**：
   - Wave 1 修完 Blocker #2 後，前端應在收到 `3002` 時觸發 refresh token 流程；收到 `3003` 直接導回登入
   - 目前 Wave 1 因 Blocker #2 未修，所有過期都會回 `3003` — 前端先依此實作即可，Wave 2 修完後再加 `3002` 分支
3. **`UNAUTHORIZED(3001)` vs `TOKEN_INVALID(3003)`**：
   - `3001` = 完全沒帶 Authorization header
   - `3003` = 有帶但解不出來
   - 兩者前端皆應導回登入頁，但**追蹤埋點需區分**
4. **`/api/v1/{module}/_status` placeholder 的 `9001` 回應**：
   - 前端若呼叫到（Wave 2 開發期）會收到 9001，請與 9001 真實系統繁忙做區分
   - Brian Minor #12 建議改成 `9003 MODULE_NOT_AVAILABLE`，定案後再請 Felix 同步前端錯誤對照表

---

## 追蹤清單

| 項目 | 檔案位置 | 問題描述 | 嚴重度 | 單元測試 | 調整完成 | 負責人 | 備註 |
|------|----------|----------|--------|----------|----------|--------|------|
| @Select 寫 INSERT | `MemberMapper.java:52` | 改 `@Insert`、回傳 int、移除 RETURNING | 🔴 嚴重 | ☐ | ☐ | Bruno | 必須加 Testcontainers 驗證 |
| JWT 例外分流 | `MemberController.java:77` `JwtTokenProvider.java:55` | 區分 ExpiredJwtException → TOKEN_EXPIRED；其餘 → TOKEN_INVALID | 🔴 嚴重 | ☐ | ☐ | Bruno | 影響 4 支 API |
| getProfile 無偏好靜默回填 | `MemberServiceImpl.java:125` | 改為 orElseThrow USER_NOT_FOUND | 🟡 中等 | ☐ | ☐ | Bruno | - |
| audit userId 全為 null | `MemberServiceImpl` `MemberController` | 登入/註冊成功後 MDC.put userId；filter finally remove | 🟡 中等 | ☐ | ☐ | Bruno | A4 履行不到位 |
| audit detail 缺 PII 遮罩 | `AuditAspect.java` `MaskUtils.java` (待建) | 對 register/login 帶遮罩 email | 🟡 中等 | ☐ | ☐ | Bruno | 合規需求 |
| local JWT secret 防誤用 | `application-local.yml`、CI guard | 加 grep guard + .env.example | 🟡 中等 | ☐ | ☐ | Bruno | - |
| LoginResponse 揭露過多 | `LoginResponse.java` | 與 Felix 對齊後決定移除 member 欄位 | 🟡 中等 | ☐ | ☐ | Bruno | 待 Felix 回覆 |
| updateProfile 未驗 affected rows + 未重讀 | `MemberServiceImpl.java:142` | rows==0 拋 USER_NOT_FOUND；成功後 findById 重讀 | 🟡 中等 | ☐ | ☐ | Bruno | - |
| ErrorCode.fromCode 改 Map | `ErrorCode.java:85` | 預建 BY_CODE Map | 🟢 輕微 | - | ☐ | Bruno | 可延後 |
| TimeUtils.toIso8601 回 Optional | `TimeUtils.java:36` | 改 Optional | 🟢 輕微 | - | ☐ | Bruno | 可延後 |
| BigDecimalUtils.fromDouble 加 @Deprecated 或抽出 | `BigDecimalUtils.java:73` | - | 🟢 輕微 | - | ☐ | Bruno | 可延後 |
| placeholder 9001 改 9003 | 9 個 PlaceholderController | 新增 ErrorCode.MODULE_NOT_AVAILABLE(9003) | 🟢 輕微 | - | ☐ | Bruno | 與 Felix 對齊 |
| docker-compose 顯式 networks | `docker-compose.yml` | - | 🟢 輕微 | - | ☐ | Bruno | 可延後 |
| ArchUnit Wave 2 第 1 週 | `stock-boot/test` | 三條規則優先導入 | 🟡 規範 | - | ☐ | Bruno | TD-5 提前 |
| Testcontainers Wave 2 第 1 週 | `stock-member/test` | MemberMapper 三支 SQL 驗證 | 🟡 規範 | - | ☐ | Bruno | TD-10 提前 |
| audit user_id null 補 TD | TD-A | - | 🟡 補登 | - | ☐ | Bruno | - |
| MaskUtils 待建 | TD-C | stock-common 新增 | 🟡 補登 | - | ☐ | Bruno | - |
| Controller 整合測試補 | TD-D | @WebMvcTest 5 支 API | 🟡 補登 | - | ☐ | Bruno | - |
| SecurityHeadersConfig 補 | TD-E | HSTS / CSP / X-Frame-Options | 🟡 補登 | - | ☐ | Bruno | Wave 2 接 Security 時 |

---

## 投票

本次 Review 無架構議題需投票（皆為實作層議題）。

---

## 結論

**狀態**：🟡 **GO-WITH-FIXES**（不阻擋 Wave 2 啟動，但 2 個 Blocker 必須在 Wave 2 第一週前完成）

**理由**：

1. **2 個 Blocker 範圍局限**（單檔註解誤用、單一 method 例外分流），預估 Bruno 修復時間 < 4 小時
2. **無 SQL Injection、無密碼明文、無跨 module 違規、無交易缺漏**等「真正阻擋」議題
3. **Envelope Pattern、業務碼分段、UUID、GMT+8、BCrypt cost 12** 等核心規範皆落實
4. Wave 2 啟動工作（Spring Security 接管、ArchUnit、Testcontainers）會自然帶出 Blocker 修復路徑
5. Bruno 開發筆記透明、TD 自陳坦率，協作信任度高

**Top 3 必修項（給 Bruno）**：

1. **🔴 #1 修正 `MemberMapper.insert()` 用 `@Insert` 註解** — 不修就有資料寫入風險
2. **🔴 #2 JWT 過期 vs 無效例外分流** — 影響 SRS §9 業務碼契約 + Wave 2 refresh token
3. **🟡 #4 audit userId 全為 null** — A4 拍板履行不到位，必須在 Wave 2 修完

**下一步**：

1. Bruno 在 Wave 2 啟動前 1-2 天修復 2 個 Blocker + #4 audit userId
2. Bruno 立即跑 `mvn clean compile` 驗證 TD-7 是否真有編譯問題
3. 跨團隊：Felix 確認 `LoginResponse.member` 是否可移除（透過 Jamie 對齊）
4. Wave 2 第 1 週優先導入 ArchUnit + Testcontainers（TD-5、TD-10 提前）
5. 二輪 Review 範圍：Blocker 修復 + Major #4/#5/#7/#8 + Wave 2 新功能

**預計第二輪 Review 時間**：Wave 2 第 1 週末（依 Jamie 排程）

---

**Review 簽核**：Brian
**簽核日期**：2026-04-22 (GMT+8)

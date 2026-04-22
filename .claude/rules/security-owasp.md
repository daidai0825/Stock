# Rule: 安全規範（OWASP Top 10）

> **適用範圍**：所有程式碼與配置
> **適用 Agents**：Bruno、Felix、Brian、Fiona、Sophia、Linus
> **參考標準**：OWASP Top 10 2021、OWASP ASVS

---

## OWASP Top 10 2021 對應

### A01:2021 - 失效的存取控制

#### 規範

- 預設拒絕（Deny by default）
- 使用 Spring Security 集中管理
- API 必須驗證權限（不只是登入）
- 禁止透過修改 URL 存取他人資料（IDOR）

```java
// ✅ 檢查使用者是否有權限存取該資源
@PostMapping("/order/get")
public ApiResponse<OrderDTO> getOrder(@RequestBody OrderRequest request,
                                       @AuthenticationPrincipal UserPrincipal user) {
    Order order = orderService.findById(request.getOrderId());
    if (!order.getUserId().equals(user.getUserId())) {
        throw new BusinessException(3002, "無權存取此訂單");
    }
    return ApiResponse.success(orderConvertor.toDTO(order));
}
```

#### 檢查清單

- [ ] 所有 endpoint 都需要認證（除了公開的）
- [ ] 資源存取檢查擁有者
- [ ] 角色權限矩陣明確
- [ ] CORS 配置嚴格（不使用 `*`）

### A02:2021 - 加密失效

#### 規範

- TLS 1.3（最低 1.2）
- 敏感資料加密儲存（AES-256）
- 密碼使用 BCrypt（cost ≥ 12）或 Argon2
- 不使用 MD5、SHA-1
- 金鑰管理：AWS KMS / HashiCorp Vault

```java
// ✅ BCrypt
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// ❌ 禁止
String hash = DigestUtils.md5Hex(password);  // MD5 已不安全
```

#### 檢查清單

- [ ] 全程 HTTPS
- [ ] 敏感欄位加密（身分證、信用卡）
- [ ] 密碼絕不明文存儲
- [ ] JWT secret 強度足夠（≥256 bit）
- [ ] 不使用過時演算法

### A03:2021 - 注入攻擊

#### SQL Injection

```java
// ✅ MyBatis 用 #{}
@Select("SELECT * FROM USER_INFO WHERE email = #{email}")
Optional<UserPO> findByEmail(String email);

// ❌ 禁止
@Select("SELECT * FROM USER_INFO WHERE email = '${email}'")
```

#### XSS

```typescript
// ❌ 禁止未 sanitize 的 HTML 渲染
<div dangerouslySetInnerHTML={{ __html: userInput }} />

// ✅ 使用 DOMPurify
import DOMPurify from 'dompurify';
<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userInput) }} />

// ✅ 最佳：純文字
<div>{userInput}</div>
```

#### Command Injection

```java
// ❌ 禁止
Runtime.getRuntime().exec("ls " + userInput);

// ✅ 使用 ProcessBuilder + 白名單驗證
if (!ALLOWED_PATHS.contains(userInput)) {
    throw new BusinessException(...);
}
new ProcessBuilder("ls", userInput).start();
```

#### 檢查清單

- [ ] 所有 SQL 使用 PreparedStatement / `#{}`
- [ ] 前端輸出 sanitize
- [ ] 輸入驗證（白名單優於黑名單）
- [ ] 檔案路徑驗證（防 path traversal）

### A04:2021 - 不安全的設計

#### 規範

- 威脅建模（Threat Modeling）
- 安全的設計模式
- 限流（Rate Limiting）
- 業務邏輯驗證（不只前端）

```java
// ✅ 後端必須驗證業務規則（不依賴前端）
@PostMapping("/order/cancel")
public ApiResponse<Void> cancelOrder(@RequestBody CancelOrderRequest request) {
    Order order = orderService.findById(request.getOrderId());

    // 驗證業務規則
    if (order.getStatus() != OrderStatus.PENDING) {
        throw new BusinessException(2002, "訂單狀態不允許取消");
    }

    orderService.cancel(order);
    return ApiResponse.success();
}
```

### A05:2021 - 安全設定錯誤

#### 規範

- 預設密碼必須變更
- 關閉不必要的功能
- 錯誤訊息不洩漏內部細節
- prod 環境關閉 Swagger UI、actuator 敏感端點

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 不暴露 env, beans 等

server:
  error:
    include-message: never
    include-stacktrace: never
    include-binding-errors: never

springdoc:
  api-docs:
    enabled: false  # prod 關閉 OpenAPI
  swagger-ui:
    enabled: false
```

#### 檢查清單

- [ ] prod 不暴露 stacktrace
- [ ] prod 關閉 Swagger UI
- [ ] CORS 不使用 `*`
- [ ] HTTP Headers 安全配置（X-Frame-Options、CSP、HSTS）

### A06:2021 - 易受攻擊與過時的元件

由 **Linus** 負責：

- 每週 OWASP Dependency Check
- 訂閱 CVE 通知
- 升級評估與排程

詳見 [Linus Agent](../agents/dev-library-linus.md) 與 [library-upgrade-evaluation skill](../skills/library-upgrade-evaluation.md)。

### A07:2021 - 認證失效

#### 規範

- 強密碼政策（≥12 字元、大小寫+數字+特殊符號）
- MFA（多因素認證）
- Account Lockout（連續失敗 5 次鎖定 15 分鐘）
- Session 管理：JWT 短時效（≤15 分鐘）+ Refresh Token
- 登入失敗訊息不明確（不告知是 email 或 password 錯誤）

```java
// ✅ 統一錯誤訊息
catch (UserNotFoundException | InvalidPasswordException e) {
    throw new BusinessException(3001, "帳號或密碼錯誤");
}
```

#### Session / JWT

```java
// JWT 配置
public class JwtConfig {
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
}
```

#### 檢查清單

- [ ] 密碼強度檢查
- [ ] 登入失敗鎖定
- [ ] MFA 支援
- [ ] Session 超時
- [ ] Logout 清除所有裝置 session（如需要）

### A08:2021 - 軟體與資料完整性失效

#### 規範

- 程式碼簽章（CI/CD artifact 簽章）
- Container image 簽章（Cosign）
- 反序列化防護（避免 `ObjectInputStream` 接受外部資料）
- 子資源完整性（SRI）：CDN 引入 JS 加 `integrity` 屬性

### A09:2021 - 安全記錄與監控失效

#### 規範

- 記錄所有認證、授權事件
- 記錄敏感操作（刪除、修改權限、轉帳）
- 不記錄敏感資料（密碼、token、信用卡完整號）
- 集中收集（CloudWatch Logs / ELK）
- 異常告警（多次登入失敗、非常時段操作）

```java
// ✅ 結構化日誌
log.info("使用者登入成功",
    StructuredArguments.kv("userId", userId),
    StructuredArguments.kv("ip", clientIp),
    StructuredArguments.kv("ua", userAgent));

// ❌ 禁止記錄敏感資料
log.info("登入請求：{}", request);  // 可能包含密碼！
```

### A10:2021 - 伺服器端請求偽造（SSRF）

#### 規範

- 對外請求使用白名單
- 禁止使用者輸入直接組成 URL
- 限制可存取的 IP 範圍（避免 metadata service `169.254.169.254`）

```java
// ✅ URL 白名單驗證
private static final Set<String> ALLOWED_HOSTS = Set.of("api.example.com", "cdn.example.com");

private void validateUrl(String url) throws MalformedURLException {
    URL parsed = new URL(url);
    if (!ALLOWED_HOSTS.contains(parsed.getHost())) {
        throw new BusinessException(1001, "不允許的 URL");
    }
}
```

## 敏感資料處理

### 不該記錄到 Log 的資料

- 密碼（含 hash）
- API Token / JWT
- Session ID
- 信用卡完整號（可記後 4 碼）
- 身分證完整號（可記後 4 碼）
- 密鑰

### 遮罩範例

```java
public class MaskUtils {
    public static String maskCreditCard(String card) {
        if (card == null || card.length() < 4) return "****";
        return "**** **** **** " + card.substring(card.length() - 4);
    }

    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at);
    }
}
```

## HTTP Security Headers

```java
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentTypeOptions(c -> {})  // X-Content-Type-Options: nosniff
            .frameOptions(f -> f.deny())   // X-Frame-Options: DENY
            .httpStrictTransportSecurity(h -> h
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
            .contentSecurityPolicy(c -> c
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'"))
            .referrerPolicy(r -> r.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        );
        return http.build();
    }
}
```

## 安全測試

由 **Quincy / Quinn** 負責：
- OWASP ZAP（基礎掃描）
- 滲透測試（季度，外部廠商）
- 模糊測試（fuzz testing）

## 合規

視業務需求遵循：
- GDPR（歐盟個資）
- 個資法（台灣）
- PCI-DSS（信用卡）
- HIPAA（醫療）

## 禁止事項

- **禁止**密碼明文存儲
- **禁止**MD5、SHA-1 用於密碼
- **禁止**SQL 字串拼接
- **禁止**`dangerouslySetInnerHTML` 未 sanitize
- **禁止**將 secret 寫入程式碼或 git
- **禁止**錯誤訊息洩漏 stack trace
- **禁止**prod 啟用 debug 端點
- **禁止**忽略 CVE 警告
- **禁止**直接信任使用者輸入

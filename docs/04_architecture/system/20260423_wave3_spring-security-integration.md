# Wave 3 Spring Security 整合策略

- **文件版本**：v1.0
- **架構師**：Sophia（資深系統架構師）
- **日期**：2026-04-23
- **適用 Wave**：Wave 3（v0.3.0），對應 PRD F-W3-04
- **上游決策**：D-2026-04-23-08（HTTP 200 + envelope code 3001）
- **規範依據**：
  - [api-design.md](../../../.claude/rules/api-design.md)
  - [security-owasp.md](../../../.claude/rules/security-owasp.md)
  - [java-spring.md](../../../.claude/rules/java-spring.md)
- **相依文件**：[20260423_wave3_system-architecture.md](20260423_wave3_system-architecture.md) §3、§5.1、§6.3

---

## 0. 設計原則

1. **Envelope Pattern 不破壞**：未授權一律 HTTP 200 + envelope `code: 3001`，**不使用 HTTP 401**
2. **Wave 2 既有 4 個查詢 endpoint 完全不受衝擊**：quote / fundamental / chip / quote/history 維持公開
3. **W2 既有 MemberController 漸進遷移**：Filter 接管 JWT 解析，Controller 端 `currentUserId()` 過渡期保留
4. **單元測試最小衝擊**：白名單 endpoint 測試零改動；受保護 endpoint 測試改用 `@WithMockUser` 或自建 JWT
5. **滲透測試友善**：所有自訂 EntryPoint / AccessDeniedHandler 設計理由寫入註解，便於廠商審查

---

## 1. Endpoint 分類矩陣

### 1.1 公開 endpoint（permitAll）

| Endpoint | 模組 | 用途 | Wave 來源 |
|----------|------|------|-----------|
| `POST /api/v1/quote/get` | stock-quote | 即時報價 | W2 |
| `POST /api/v1/quote/list` | stock-quote | 批次報價 | W2 |
| `POST /api/v1/quote/history` | stock-quote | K 線歷史 | W2 |
| `POST /api/v1/fundamental/get` | stock-fundamental | 基本面 | W2 |
| `POST /api/v1/chip/get` | stock-chip | 籌碼 | W2 |
| `POST /api/v1/member/register` | stock-member | 註冊 | W2 |
| `POST /api/v1/member/login` | stock-member | 登入 | W2 |
| `POST /api/v1/member/refresh` | stock-member | refresh token（W3 啟用實作）| W2 |
| `POST /api/v1/search/stock` | stock-search（W3 新增）| 模糊搜尋（基礎功能）| W3 |
| `POST /api/v1/search/popular` | stock-search | 熱門搜尋 | W3 |
| `GET /actuator/health/**` | stock-boot | K8s probe | W2 |
| `GET /v3/api-docs/**`、`GET /swagger-ui/**` | springdoc | local/dev/uat 開放，prod 關閉 | W2 |

### 1.2 受保護 endpoint（authenticated）

| Endpoint | 模組 | 是否 IDOR 檢查 | Wave 來源 |
|----------|------|----------------|-----------|
| `POST /api/v1/member/logout` | stock-member | userId from JWT | W2 已存在，W3 改 Filter |
| `POST /api/v1/member/profile/get` | stock-member | userId from JWT | W2 已存在，W3 改 Filter |
| `POST /api/v1/member/profile/update` | stock-member | userId from JWT | W2 已存在，W3 改 Filter |
| `POST /api/v1/watchlist/add` | stock-watchlist | userId from JWT | W3 |
| `POST /api/v1/watchlist/list` | stock-watchlist | userId from JWT | W3 |
| `POST /api/v1/watchlist/remove` | stock-watchlist | userId from JWT | W3 |
| `POST /api/v1/alert/create` | stock-notify | userId from JWT | W3 |
| `POST /api/v1/alert/list` | stock-notify | userId from JWT | W3 |
| `POST /api/v1/alert/update` | stock-notify | 需檢查 alert.userId == JWT.userId | W3 |
| `POST /api/v1/alert/delete` | stock-notify | 同上 | W3 |
| `POST /api/v1/push/subscription/register` | stock-notify | userId from JWT | W3 |
| `POST /api/v1/push/subscription/unregister` | stock-notify | userId from JWT | W3 |
| `POST /api/v1/search/history/list` | stock-search | userId from JWT | W3 |
| `POST /api/v1/search/history/clear` | stock-search | userId from JWT | W3 |

### 1.3 內部 endpoint（IP 限制 + IAM SigV4）

| Endpoint | 呼叫者 | 認證 |
|----------|--------|------|
| `POST /internal/alert/scan` | Lambda alert-evaluator-trigger | IAM SigV4 + 來源 IP 白名單 |
| `POST /internal/health/deep` | ALB | 來源 IP 白名單 |

`/internal/*` 由 ALB listener rule path-based 限制（**只允許來自 internal target group**），且 SecurityConfig 內 `requestMatchers("/internal/**").access(...)` 使用 IP 來源檢查 + custom `InternalAuthenticationFilter`。

---

## 2. Spring Security FilterChain 設計

### 2.1 SecurityConfig（建議放 `stock-common/security/`，由 Bruno 實作）

```java
package tw.com.stockplatform.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Wave 3 F-W3-04 核心：
 * 1. 公開 endpoint 白名單（不打 W2 既有 controller）
 * 2. 受保護 endpoint 走 JwtAuthenticationFilter
 * 3. 自訂 EntryPoint / AccessDeniedHandler 回 HTTP 200 + envelope（D-08 拍板）
 * 4. CSRF 關閉（採 Bearer Token 而非 Cookie session）
 * 5. SessionCreationPolicy.STATELESS（不建 HttpSession）
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(apiAuthenticationEntryPoint)  // 401 場景
                .accessDeniedHandler(apiAccessDeniedHandler)            // 403 場景
            )
            .authorizeHttpRequests(auth -> auth
                // === 公開 endpoint（W2 既有 + W3 新增基礎搜尋）===
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/quote/get",
                    "/api/v1/quote/list",
                    "/api/v1/quote/history",
                    "/api/v1/fundamental/get",
                    "/api/v1/chip/get",
                    "/api/v1/member/register",
                    "/api/v1/member/login",
                    "/api/v1/member/refresh",
                    "/api/v1/search/stock",
                    "/api/v1/search/popular"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // === 內部 endpoint（IP + 自訂 filter）===
                .requestMatchers("/internal/**").access(InternalAccessVoter.allow())

                // === 其餘 /api/v1/** 一律需要認證 ===
                .requestMatchers("/api/v1/**").authenticated()

                // === 預設 deny ===
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h
                .contentTypeOptions(c -> {})
                .frameOptions(f -> f.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(r -> r.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt cost 12（OWASP 建議 ≥ 10，金融類取 12）
        return new BCryptPasswordEncoder(12);
    }
}
```

### 2.2 JwtAuthenticationFilter

```java
package tw.com.stockplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tw.com.stockplatform.common.trace.TraceIdFilter;
import tw.com.stockplatform.member.security.JwtTokenProvider;
import tw.com.stockplatform.member.security.exception.JwtExpiredException;
import tw.com.stockplatform.member.security.exception.JwtInvalidException;

import java.io.IOException;
import java.util.Collections;

/**
 * 解析 Authorization: Bearer xxx，建立 SecurityContext。
 * <p>
 * 解析失敗（過期 / 無效）時：
 * - 不直接寫 response，而是清空 context 並繼續走 filter chain
 * - 後續 ExceptionTranslationFilter 觸發 ApiAuthenticationEntryPoint
 * - EntryPoint 依 MDC 中的 jwtErrorCode 判斷回 3001 / 3002 / 3003
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    static final String JWT_ERROR_ATTR = "jwtAuthError";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            var claims = jwtTokenProvider.parse(token);
            var user = new AuthenticatedUser(claims.getSubject(), (String) claims.get("email"));
            var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            MDC.put(TraceIdFilter.USER_ID_MDC_KEY, user.userId());
        } catch (JwtExpiredException ex) {
            request.setAttribute(JWT_ERROR_ATTR, "TOKEN_EXPIRED");
        } catch (JwtInvalidException ex) {
            request.setAttribute(JWT_ERROR_ATTR, "TOKEN_INVALID");
        }
        chain.doFilter(request, response);
    }
}
```

### 2.3 ApiAuthenticationEntryPoint（D-08 核心實作）

```java
package tw.com.stockplatform.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.response.ApiResponse;

import java.io.IOException;

/**
 * D-08 拍板：未授權一律 HTTP 200 + envelope code 3001 / 3002 / 3003。
 * <p>
 * 取代 Spring Security 預設行為（401 + WWW-Authenticate header）。
 * <p>
 * 滲透測試說明（W6）：本設計符合本平台 api-design.md Envelope Pattern，非漏洞。
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode code = resolveErrorCode(request);
        ApiResponse<Void> body = ApiResponse.fail(code);

        response.setStatus(HttpStatus.OK.value());                // ★ 一律 HTTP 200
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        String jwtError = (String) request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTR);
        if ("TOKEN_EXPIRED".equals(jwtError)) {
            return ErrorCode.TOKEN_EXPIRED;       // 3002
        }
        if ("TOKEN_INVALID".equals(jwtError)) {
            return ErrorCode.TOKEN_INVALID;       // 3003
        }
        return ErrorCode.UNAUTHORIZED;            // 3001
    }
}
```

### 2.4 ApiAccessDeniedHandler

```java
package tw.com.stockplatform.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.response.ApiResponse;

import java.io.IOException;

/**
 * 已登入但無權限的場景（例如 IDOR、未啟用功能）。
 * 回 HTTP 200 + code 3004 FORBIDDEN。
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.FORBIDDEN);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
```

### 2.5 AuthenticatedUser

```java
package tw.com.stockplatform.common.security;

/**
 * Java 21 record，作為 Spring Security Principal。
 * Controller 可注入 @AuthenticationPrincipal AuthenticatedUser。
 */
public record AuthenticatedUser(String userId, String email) { }
```

---

## 3. Refresh Token + HttpOnly Cookie 設計

### 3.1 Token TTL

| Token | TTL | 儲存位置 | 用途 |
|-------|-----|----------|------|
| Access Token (JWT) | 15 min | 前端記憶體（**禁存 localStorage**）| API 認證 |
| Refresh Token | 7 day | **HttpOnly + Secure + SameSite=Lax Cookie** | 換發 access token |

### 3.2 Refresh Token 流程

```mermaid
sequenceDiagram
    participant SPA
    participant ALB
    participant Auth as MemberController
    participant Redis
    participant DB

    Note over SPA,DB: 登入
    SPA->>Auth: POST /member/login {email, password}
    Auth->>DB: 驗證
    Auth->>Redis: SETEX refresh:{userId}:{tokenId} 7d {hash}
    Auth-->>SPA: ApiResponse(data={accessToken})<br/>Set-Cookie: refresh_token=xxx; HttpOnly; Secure; Path=/api/v1/member/refresh

    Note over SPA,DB: Access Token 過期
    SPA->>ALB: POST /api/v1/watchlist/list (expired)
    ALB-->>SPA: code: 3002 (TOKEN_EXPIRED)
    SPA->>Auth: POST /member/refresh (Cookie 自動帶)
    Auth->>Redis: GET refresh:{userId}:{tokenId}
    alt 比對成功
        Auth->>Redis: DEL 舊 + SETEX 新（rotation）
        Auth-->>SPA: ApiResponse(data={accessToken})<br/>Set-Cookie: refresh_token=新值
        SPA->>SPA: 重試原請求
    else 比對失敗（reuse 攻擊）
        Auth->>Redis: DEL all refresh:{userId}:* (revoke 所有 session)
        Auth-->>SPA: code: 3003 (TOKEN_INVALID)
        SPA->>SPA: 導向登入頁
    end
```

### 3.3 為什麼 Refresh Token 用 HttpOnly Cookie？

| 方案 | 優點 | 缺點 | 採用 |
|------|------|------|------|
| localStorage | 容易實作 | XSS 可竊（OWASP A03）、刷新頁面後仍存在 | ❌ |
| sessionStorage | 同上 + 關頁清除 | XSS 可竊 | ❌ |
| **HttpOnly Cookie** | XSS 無法讀、自動帶送 | 需處理 CSRF（但 refresh endpoint 唯一動作不會造成資料變更，可豁免）| ✅ |
| 純記憶體 | 安全 | 重整網頁丟失，UX 差 | ❌ |

**CSRF 緩解**：refresh endpoint 限定 `SameSite=Lax`、僅接受 POST，且 endpoint 唯一輸出新 access token（不執行業務動作）。

### 3.4 Cookie 設定

```
Set-Cookie: refresh_token={token};
            Path=/api/v1/member/refresh;
            HttpOnly;
            Secure;
            SameSite=Lax;
            Max-Age=604800
```

---

## 4. Wave 2 既有 MemberController 遷移計劃

### 4.1 改動範圍

| 既有方法 | W3 處理 |
|----------|---------|
| `currentUserId(HttpServletRequest)` | 標記 `@Deprecated`，註解保留 1 wave；新方法改注入 `@AuthenticationPrincipal AuthenticatedUser user` |
| `MemberExceptionHandler` 內 `@ExceptionHandler(JwtExpiredException)` | 保留（過渡期），同時新增 EntryPoint 路徑 |

### 4.2 遷移後範例

```java
// W3 改寫範例
@PostMapping("/profile/get")
public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
    return ApiResponse.success(memberService.getProfile(user.userId()));
}

@PostMapping("/profile/update")
public ApiResponse<MemberDTO> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
    return ApiResponse.success(memberService.updateProfile(user.userId(), request));
}
```

### 4.3 既有測試衝擊

| 測試類別 | 改動方式 |
|----------|----------|
| `QuoteControllerTest`（W2）| **零改動**（permitAll）|
| `ChipControllerTest`、`FundamentalControllerTest`（W2）| **零改動**（permitAll）|
| `MemberControllerTest`（W2）| 加 `@Import(SecurityConfig.class)` + `@WithMockUser` 或 mock JWT |
| `MemberControllerExceptionTest`（W2 既有 401 流程）| 改斷言路徑：原本 throw `BusinessException` → 改驗證 EntryPoint 寫出的 envelope |

**Brian Code Review 必要檢查項目**：
1. W2 5 個 endpoint 的 MockMvc 測試覆蓋率不下降
2. 新增 `SecurityConfigIntegrationTest` 驗證白名單 + 受保護分類正確
3. `@WithMockUser` 設定的 userId 必須對應到測試 DB seed

---

## 5. 整合測試策略

### 5.1 測試矩陣

| 測試 | 工具 | 必要場景 |
|------|------|----------|
| Unit | JUnit 5 + Mockito | Filter / EntryPoint / Handler 隔離測試 |
| Integration | `@SpringBootTest` + Testcontainers PostgreSQL + RestAssured | 完整 FilterChain 跑通 |
| Security | `spring-security-test` + `@WithMockUser` | 受保護 endpoint 認證行為 |
| API contract | RestAssured + JSON Schema | envelope `code` 對應 HTTP 200 |
| OWASP ZAP | 自動化 baseline scan（CI 整合）| 0 High / 0 Critical |

### 5.2 必跑場景

```
SC-SEC-01: 公開 endpoint 不帶 JWT → HTTP 200 + 業務 success
SC-SEC-02: 受保護 endpoint 不帶 JWT → HTTP 200 + code 3001
SC-SEC-03: 受保護 endpoint 帶過期 JWT → HTTP 200 + code 3002
SC-SEC-04: 受保護 endpoint 帶簽章錯誤 JWT → HTTP 200 + code 3003
SC-SEC-05: 受保護 endpoint 帶有效 JWT → HTTP 200 + 業務 success
SC-SEC-06: IDOR 嘗試（userA 改 userB watchlist）→ HTTP 200 + code 3004
SC-SEC-07: refresh 成功 → 新 access token + 新 cookie
SC-SEC-08: refresh 重放（用舊 token 二次）→ revoke 全 session + code 3003
SC-SEC-09: 內部 endpoint 從外網呼叫 → HTTP 200 + code 3001
SC-SEC-10: 內部 endpoint 從 Lambda 呼叫 → HTTP 200 + 業務 success
```

---

## 6. 滲透測試廠商說明文件（隨報告附上）

> 提供給 W6 滲透測試廠商，避免 false positive。

### 6.1 設計決策說明

1. **未授權回 HTTP 200**：本平台採 [Envelope Pattern](https://datatracker.ietf.org/doc/html/draft-snell-http-prefer-18) 的延伸設計，所有 API 一律 HTTP 200，業務狀態以 envelope `code` 表達。對應決策紀錄 [D-2026-04-23-08](../../01_leader/decisions/20260423_decision-wave2-wave3-9items.md)。
2. **Refresh Token 用 HttpOnly Cookie**：避免 XSS 竊取，符合 OWASP A02 / A03。Access Token 不寫入 localStorage。
3. **CSRF 關閉**：採 Bearer Token 而非 session cookie；唯一使用 cookie 的 endpoint（`/member/refresh`）為 idempotent token rotation，且 `SameSite=Lax`。
4. **HSTS / X-Frame-Options / Referrer-Policy** 已啟用，可在 response header 驗證。

### 6.2 預期被質疑但已對應的點

| 廠商可能標 finding | 平台說明 |
|--------------------|----------|
| 未授權應回 401 | 本平台 API 設計規範統一 200 + envelope code，前端與後端皆已對齊 |
| JWT in Authorization Header 可被 XSS 竊取 | Access Token 僅存記憶體，無 localStorage / sessionStorage 持久化 |
| Refresh Token 暴露在 cookie | HttpOnly + Secure + SameSite，限定 Path=/api/v1/member/refresh |
| 缺 MFA | Wave 3 未支援，已列入 v1.0 roadmap，本次測試範圍排除 |

---

## 7. 開發任務分派（給 Bruno / Brian）

| # | 任務 | Owner | 預估 |
|---|------|-------|------|
| T-SEC-01 | 新增 `stock-common/security/` 套件 + 5 個檔案（SecurityConfig / Filter / EntryPoint / DeniedHandler / Principal）| Bruno | 1 day |
| T-SEC-02 | 修改 `MemberController` 使用 `@AuthenticationPrincipal`，並保留舊 `currentUserId()` 為 deprecated | Bruno | 0.5 day |
| T-SEC-03 | Refresh Token Service（Redis + rotation + reuse detection）| Bruno | 1.5 day |
| T-SEC-04 | Account Lockout（5 次 / 15 min Redis counter）| Bruno | 0.5 day |
| T-SEC-05 | 新 W3 受保護 endpoint 全部加 `@AuthenticationPrincipal` 注入 | Bruno | 隨 W3 模組開發 |
| T-SEC-06 | 整合測試 SC-SEC-01 ~ SC-SEC-10 | Bruno | 1 day |
| T-SEC-07 | OWASP ZAP CI baseline 整合 | Bruno + Quincy | 0.5 day |
| T-SEC-08 | Code Review checklist 補充（IDOR、Authentication、Refresh）| Brian | 0.5 day |
| T-SEC-09 | 滲透測試說明文件中文 + 英文版（廠商交付）| Sophia | 0.5 day |

**總計**：~6 day（含跨模組整合 buffer）

---

## 8. 風險

| 風險 | 緩解 |
|------|------|
| 自寫 EntryPoint 漏處理某些 AuthenticationException 子類 | 整合測試覆蓋所有 Filter 異常路徑（SC-SEC-02 ~ 04）|
| `@AuthenticationPrincipal` 若 Filter 沒設 SecurityContext 會注入 null | Filter 設定明確；新 W3 Controller 全部 `@AuthenticationPrincipal AuthenticatedUser` 為 `@NotNull` 校驗 |
| Refresh token reuse 偵測邏輯複雜 | 採 token rotation + Redis 記錄 used token，5 分鐘 grace period |
| BCrypt cost 12 在登入時可能 ~200ms | 可接受（登入頻率低，且抗暴力破解需要）|

---

**本文件為 Spring Security 整合的最終藍本，Bruno 開發 / Brian Review 均以本文件為準**

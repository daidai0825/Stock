# Wave 3 模組拆解（套件結構與類別清單）

> **文件版本**：v1.0
> **撰寫者**：Preston
> **撰寫日期**：2026-04-23
> **配套文件**：[Project Architecture](20260423_wave3_project-architecture.md)、[ER Diagram](20260423_wave3_er-diagram.md)、[Flyway Migration](20260423_wave3_flyway-migration-plan.md)

---

## 0. 命名與套件慣例

- **base package**：`tw.com.stockplatform.{module-short-name}`（沿用 Wave 1/2 慣例）
- **DTO 命名**：Request 結尾 / Response 結尾 / 一般 DTO 結尾（沿用既有）
- **PO 命名**：`{Entity}PO`
- **Mapper 命名**：`{Entity}Mapper`（MyBatis interface）
- **Service**：interface `{Domain}Service` + impl `{Domain}ServiceImpl`
- **Convertor**：MapStruct interface `{Domain}Convertor`
- **Controller**：`{Domain}Controller`
- **Exception**：`{Domain}{Reason}Exception` extends `BusinessException`

---

## 1. stock-member（✏ Wave 3 擴充）

### 1.1 新增 / 變更檔案

```
stock-member/
└── src/main/java/tw/com/stockplatform/member/
    ├── config/
    │   ├── MemberAutoConfig.java                       (既有)
    │   └── SecurityProperties.java                     ✨新增 @ConfigurationProperties("stock.member")
    ├── controller/
    │   ├── MemberController.java                       (既有)
    │   └── AuthLogoutController.java                   ✨新增 (POST /api/v1/auth/logout)
    ├── service/
    │   ├── MemberService.java                          (既有)
    │   ├── RefreshTokenService.java                    ✨新增 interface
    │   ├── JwtBlacklistService.java                    ✨新增 interface
    │   └── impl/
    │       ├── MemberServiceImpl.java                  (既有, Wave 3 擴充 logout 流程)
    │       ├── RefreshTokenServiceImpl.java            ✨新增
    │       └── JwtBlacklistServiceImpl.java            ✨新增
    ├── repository/
    │   ├── MemberMapper.java                           (既有)
    │   ├── UserPreferenceMapper.java                   (既有)
    │   └── RefreshTokenMapper.java                     ✨新增
    ├── po/
    │   └── RefreshTokenPO.java                         ✨新增
    ├── dto/
    │   ├── request/
    │   │   ├── LogoutRequest.java                      ✨新增
    │   │   └── (既有 Login/Refresh/Register/Profile)
    │   └── response/
    │       └── (既有)
    ├── security/
    │   ├── JwtTokenProvider.java                       (既有, Wave 3 擴充 blacklist 檢查)
    │   ├── PasswordEncoder.java                        (既有)
    │   ├── JwtAuthenticationFilter.java                ✨新增 (Spring Security Filter)
    │   ├── JwtAuthenticationEntryPoint.java            ✨新增 (D-08 envelope code 3001)
    │   ├── RestAccessDeniedHandler.java                ✨新增 (envelope code 3002)
    │   ├── UserPrincipal.java                          ✨新增 implements UserDetails
    │   └── exception/
    │       ├── JwtAuthException.java                   (既有)
    │       ├── JwtExpiredException.java                (既有)
    │       ├── JwtInvalidException.java                (既有)
    │       └── JwtBlacklistedException.java            ✨新增
    └── exception/
        └── MemberExceptionHandler.java                 (既有)
```

### 1.2 關鍵類別骨架

```java
// JwtAuthenticationEntryPoint：未授權統一輸出 envelope code 3001（D-08）
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String traceId = MDC.get("traceId");
        ApiResponse<Void> body = ApiResponse.error(3001, "未登入", traceId);
        response.setStatus(HttpStatus.OK.value());        // D-08：HTTP 200
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
```

```java
// RefreshTokenPO：對應 refresh_token 表
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenPO {
    private String tokenId;        // UUID
    private String userId;
    private String tokenHash;      // SHA-256 of refresh token
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revokedReason;  // LOGOUT / REFRESH_USED / FORCE_REVOKE
}
```

### 1.3 對外 API（變更 / 新增）

| Method | Path | Auth | 說明 |
|--------|------|------|------|
| POST | `/api/v1/auth/logout` | ✅ Required | 登出，blacklist access token + refresh token |
| POST | `/api/v1/auth/login` | 公開 | （既有，Wave 3 改為持久化 refresh token） |
| POST | `/api/v1/auth/refresh` | 公開 | （既有，Wave 3 改為從表查 refresh token） |

---

## 2. stock-watchlist（✏ Wave 3 落地）

### 2.1 套件結構

```
stock-watchlist/
└── src/main/java/tw/com/stockplatform/watchlist/
    ├── config/
    │   ├── WatchlistAutoConfig.java                    (既有，Wave 3 擴充)
    │   └── WatchlistProperties.java                    ✨新增 (max-per-user)
    ├── controller/
    │   └── WatchlistController.java                    ✨新增 (取代 placeholder)
    ├── service/
    │   ├── WatchlistService.java                       ✨新增 interface
    │   └── impl/
    │       └── WatchlistServiceImpl.java               ✨新增
    ├── repository/
    │   └── WatchlistMapper.java                        ✨新增
    ├── po/
    │   └── WatchlistPO.java                            ✨新增
    ├── dto/
    │   ├── request/
    │   │   ├── WatchlistAddRequest.java                ✨新增 record
    │   │   ├── WatchlistRemoveRequest.java             ✨新增 record
    │   │   └── WatchlistListRequest.java               ✨新增 record (空 body 也可)
    │   └── response/
    │       ├── WatchlistDTO.java                       ✨新增 record (單筆)
    │       └── WatchlistAggregateDTO.java              ✨新增 record (含報價彙整)
    ├── convertor/
    │   └── WatchlistConvertor.java                     ✨新增 MapStruct
    └── exception/
        ├── WatchlistLimitExceededException.java        ✨新增 (code 4101)
        └── WatchlistDuplicateException.java            ✨新增 (code 4102)
```

### 2.2 關鍵類別

```java
// WatchlistPO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistPO {
    private String watchlistId;   // UUID
    private String userId;
    private String stockId;
    private Integer sortOrder;    // 顯示順序 1..N
    private LocalDateTime createdAt;
}

// WatchlistAggregateDTO（給彙整 API 用）
public record WatchlistAggregateDTO(
    String stockId,
    String stockName,
    BigDecimal currentPrice,
    BigDecimal change,
    BigDecimal changePercent,
    Long volume,
    LocalDateTime quoteAt
) {}
```

### 2.3 對外 API

| Method | Path | Auth | Body | 說明 |
|--------|------|------|------|------|
| POST | `/api/v1/watchlist/add` | ✅ | `{stockId}` | 加入自選 |
| POST | `/api/v1/watchlist/remove` | ✅ | `{stockId}` | 移除自選 |
| POST | `/api/v1/watchlist/list` | ✅ | `{}` | 列表 + 報價彙整 |

### 2.4 跨模組依賴

- 呼叫 `stock-quote.QuoteService.batchListLatestQuotes(List<String>)`（**未實作須請 Bruno 補**，呼應跨Wave-7 N+1 優化）

---

## 3. stock-search（✨ Wave 3 新增）

### 3.1 套件結構

```
stock-search/
├── pom.xml                                             ✨新建
└── src/main/java/tw/com/stockplatform/search/
    ├── config/
    │   ├── SearchAutoConfig.java                       ✨ @AutoConfiguration
    │   └── SearchProperties.java                       ✨ stock.search.*
    ├── controller/
    │   └── SearchController.java                       ✨
    ├── service/
    │   ├── SearchService.java                          ✨ interface
    │   ├── SearchHistoryService.java                   ✨ interface
    │   ├── HotSearchService.java                       ✨ interface
    │   ├── StockInfoSyncService.java                   ✨ interface (TWSE+OTC 主檔同步)
    │   ├── HotSearchAggregator.java                    ✨ @Scheduled 每小時聚合
    │   └── impl/
    │       ├── SearchServiceImpl.java                  ✨
    │       ├── SearchHistoryServiceImpl.java           ✨
    │       ├── HotSearchServiceImpl.java               ✨
    │       └── StockInfoSyncServiceImpl.java           ✨
    ├── repository/
    │   ├── StockInfoMapper.java                        ✨
    │   ├── SearchHistoryMapper.java                    ✨
    │   └── HotSearchMapper.java                        ✨
    ├── po/
    │   ├── StockInfoPO.java                            ✨
    │   ├── SearchHistoryPO.java                        ✨
    │   └── HotSearchPO.java                            ✨
    ├── dto/
    │   ├── request/
    │   │   ├── StockSearchRequest.java                 ✨ record
    │   │   ├── SearchHistoryRemoveRequest.java         ✨ record
    │   │   └── (HotSearchListRequest 不需 body)
    │   └── response/
    │       ├── StockSearchItemDTO.java                 ✨ record
    │       ├── SearchHistoryItemDTO.java               ✨ record
    │       └── HotSearchItemDTO.java                   ✨ record
    ├── convertor/
    │   └── SearchConvertor.java                        ✨ MapStruct
    └── exception/
        ├── SearchKeywordInvalidException.java          ✨ (code 1101)
        └── StockNotFoundException.java                 ✨ (code 4001)
```

### 3.2 關鍵類別

```java
// StockInfoPO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoPO {
    private String stockId;        // 2330
    private String stockName;      // 台積電
    private String stockNameEn;    // TSMC（可空）
    private String market;         // TWSE / OTC
    private String industry;       // 半導體（可空，後續豐富）
    private String status;         // ACTIVE / DELISTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// SearchHistoryPO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryPO {
    private String historyId;      // UUID
    private String userId;
    private String keyword;        // 原始關鍵字
    private String resultStockId;  // 點擊後最終進入的 stockId（可空：未點擊只搜尋）
    private LocalDateTime searchedAt;
}

// SearchService.search() 實作要點
public List<StockSearchItemDTO> search(String keyword, int limit) {
    if (keyword == null || keyword.isBlank()) {
        throw new SearchKeywordInvalidException();
    }
    String trimmed = keyword.trim();
    if (trimmed.length() > 20) {
        throw new SearchKeywordInvalidException();
    }
    // 排序權重：完全匹配 > 前綴匹配 > pg_trgm similarity
    return stockInfoMapper.searchByKeyword(trimmed, limit);
}
```

### 3.3 對外 API

| Method | Path | Auth | Body | 說明 |
|--------|------|------|------|------|
| POST | `/api/v1/search/stock` | 公開 | `{keyword, limit?}` | 主搜尋 |
| POST | `/api/v1/search/hot` | 公開 | `{}` | 熱門搜尋 top 10 |
| POST | `/api/v1/search/history/list` | ✅ | `{}` | 個人歷史 |
| POST | `/api/v1/search/history/remove` | ✅ | `{historyId}` | 移除單筆 |
| POST | `/api/v1/search/history/clear` | ✅ | `{}` | 清空 |

### 3.4 排程（@Scheduled）

| Bean | Cron | 用途 |
|------|------|------|
| `HotSearchAggregator` | `0 0 * * * *`（每小時整點） | 從 search_history 聚合 top 10，寫入 hot_search 表（隱私：count < 3 不入榜） |
| `StockInfoSyncService` | `0 30 18 * * MON-FRI`（盤後 18:30） | TWSE/OTC 主檔每日同步（增量） |

---

## 4. stock-alert（✨ Wave 3 新增）

### 4.1 套件結構

```
stock-alert/
├── pom.xml                                             ✨新建
└── src/main/java/tw/com/stockplatform/alert/
    ├── config/
    │   ├── AlertAutoConfig.java                        ✨ @AutoConfiguration
    │   └── AlertProperties.java                        ✨ stock.alert.*
    ├── controller/
    │   └── AlertController.java                        ✨
    ├── service/
    │   ├── AlertService.java                           ✨ interface (CRUD)
    │   ├── AlertEvaluationService.java                 ✨ interface (觸發判斷)
    │   ├── AlertTriggerEngine.java                     ✨ @Scheduled 每分鐘掃描
    │   ├── TradingCalendarService.java                 ✨ interface (TD-7 整合)
    │   ├── strategy/
    │   │   ├── AlertEvaluator.java                     ✨ interface
    │   │   ├── AlertEvaluatorFactory.java              ✨ @Component
    │   │   ├── PriceBreakUpEvaluator.java              ✨ Strategy
    │   │   ├── PriceBreakDownEvaluator.java            ✨ Strategy
    │   │   └── DailyChangeEvaluator.java               ✨ Strategy
    │   ├── event/
    │   │   ├── AlertTriggeredEvent.java                ✨ record (Spring Event)
    │   │   └── AlertTriggeredEventListener.java        ✨ 呼叫 stock-notify
    │   └── impl/
    │       ├── AlertServiceImpl.java                   ✨
    │       ├── AlertEvaluationServiceImpl.java         ✨
    │       └── TradingCalendarServiceImpl.java         ✨ (硬編 + 假日 API)
    ├── repository/
    │   ├── PriceAlertMapper.java                       ✨
    │   └── AlertTriggerLogMapper.java                  ✨
    ├── po/
    │   ├── PriceAlertPO.java                           ✨
    │   └── AlertTriggerLogPO.java                      ✨
    ├── enums/
    │   ├── AlertType.java                              ✨ PRICE_BREAK_UP / PRICE_BREAK_DOWN / DAILY_CHANGE_PCT
    │   └── AlertStatus.java                            ✨ ACTIVE / TRIGGERED / DISABLED / DELETED
    ├── dto/
    │   ├── request/
    │   │   ├── AlertCreateRequest.java                 ✨ record
    │   │   ├── AlertUpdateRequest.java                 ✨ record
    │   │   ├── AlertDeleteRequest.java                 ✨ record
    │   │   └── AlertListRequest.java                   ✨ record
    │   └── response/
    │       └── AlertDTO.java                           ✨ record
    ├── convertor/
    │   └── AlertConvertor.java                         ✨ MapStruct
    └── exception/
        ├── AlertLimitExceededException.java            ✨ (code 4201)
        ├── AlertNotFoundException.java                 ✨ (code 4202)
        └── AlertInvalidThresholdException.java         ✨ (code 1201)
```

### 4.2 關鍵類別

```java
// PriceAlertPO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertPO {
    private String alertId;        // UUID
    private String userId;
    private String stockId;
    private AlertType alertType;   // PRICE_BREAK_UP / DOWN / DAILY_CHANGE_PCT
    private BigDecimal threshold;  // 突破價位 或 漲跌幅 %
    private AlertStatus status;
    private LocalDateTime triggeredAt;  // 最近一次觸發時間
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// AlertTriggerLogPO（含 idempotency）
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTriggerLogPO {
    private String triggerId;      // UUID
    private String alertId;
    private String userId;
    private String stockId;
    private LocalDate tradeDate;   // idempotency key 一部分
    private String idempotencyKey; // 例：{alertId}-{tradeDate} (UNIQUE)
    private BigDecimal triggeredPrice;
    private LocalDateTime triggeredAt;
    private String dispatchStatus; // PENDING / DISPATCHED / FAILED
}

// AlertEvaluator Strategy
public interface AlertEvaluator {
    boolean evaluate(PriceAlertPO alert, BigDecimal latestPrice, BigDecimal previousClose);
    AlertType supports();
}

@Component
@RequiredArgsConstructor
public class PriceBreakDownEvaluator implements AlertEvaluator {
    @Override
    public boolean evaluate(PriceAlertPO alert, BigDecimal latestPrice, BigDecimal previousClose) {
        // 「向下突破」：上一刻 ≥ threshold 且當前 < threshold
        return latestPrice.compareTo(alert.getThreshold()) < 0;
    }
    @Override public AlertType supports() { return AlertType.PRICE_BREAK_DOWN; }
}

// AlertTriggerEngine
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertTriggerEngine {
    private final TradingCalendarService calendar;
    private final AlertEvaluationService evaluationService;
    private final ApplicationEventPublisher publisher;

    @Scheduled(cron = "${stock.alert.scheduler.cron}")
    public void tick() {
        if (!calendar.isTradingDayNow()) {
            return;
        }
        List<AlertTriggeredEvent> triggers = evaluationService.evaluateAllActive();
        triggers.forEach(publisher::publishEvent);
    }
}
```

### 4.3 對外 API

| Method | Path | Auth | Body | 說明 |
|--------|------|------|------|------|
| POST | `/api/v1/alert/create` | ✅ | `{stockId, alertType, threshold}` | 新增警示（每股最多 5 條） |
| POST | `/api/v1/alert/update` | ✅ | `{alertId, threshold?, status?}` | 更新（可啟用 / 停用） |
| POST | `/api/v1/alert/delete` | ✅ | `{alertId}` | 刪除 |
| POST | `/api/v1/alert/list` | ✅ | `{stockId?, status?}` | 列表 |

---

## 5. stock-notify（✏ Wave 3 落地）

### 5.1 套件結構

```
stock-notify/
└── src/main/java/tw/com/stockplatform/notify/
    ├── config/
    │   ├── NotifyAutoConfig.java                       (既有, Wave 3 擴充)
    │   ├── NotifyProperties.java                       ✨新增
    │   ├── WebPushConfig.java                          ✨新增 (VAPID Bean)
    │   └── SesClientConfig.java                        ✨新增 (AWS SES SDK)
    ├── controller/
    │   └── NotifyController.java                       ✨新增 (取代 placeholder)
    ├── service/
    │   ├── NotificationDispatcher.java                 ✨ interface (給 stock-alert 用)
    │   ├── PushSubscriptionService.java                ✨ interface
    │   ├── WebPushService.java                         ✨ interface
    │   ├── EmailService.java                           ✨ interface
    │   ├── channel/
    │   │   ├── NotificationChannel.java                ✨ interface (Strategy)
    │   │   ├── AbstractNotificationChannel.java        ✨ Template Method
    │   │   ├── WebPushChannel.java                     ✨ Strategy
    │   │   └── EmailChannel.java                       ✨ Strategy
    │   └── impl/
    │       ├── NotificationDispatcherImpl.java         ✨
    │       ├── PushSubscriptionServiceImpl.java        ✨
    │       ├── WebPushServiceImpl.java                 ✨
    │       └── EmailServiceImpl.java                   ✨
    ├── repository/
    │   ├── PushSubscriptionMapper.java                 ✨
    │   └── NotificationLogMapper.java                  ✨
    ├── po/
    │   ├── PushSubscriptionPO.java                     ✨
    │   └── NotificationLogPO.java                      ✨
    ├── enums/
    │   ├── NotifyChannel.java                          ✨ WEB_PUSH / EMAIL
    │   └── NotifyStatus.java                           ✨ PENDING / SENT / FAILED / EXPIRED
    ├── dto/
    │   ├── request/
    │   │   ├── PushSubscribeRequest.java               ✨ record (endpoint, p256dh, auth)
    │   │   ├── PushUnsubscribeRequest.java             ✨ record
    │   │   ├── NotifyTestRequest.java                  ✨ record
    │   │   └── NotificationRequest.java                ✨ record (內部，給 dispatcher)
    │   └── response/
    │       └── PushSubscriptionDTO.java                ✨ record
    ├── convertor/
    │   └── NotifyConvertor.java                        ✨ MapStruct
    └── exception/
        ├── PushSubscriptionExpiredException.java       ✨ (code 5101)
        └── NotificationDispatchFailedException.java    ✨ (code 5102)
```

### 5.2 關鍵類別

```java
// PushSubscriptionPO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionPO {
    private String subscriptionId;  // UUID
    private String userId;
    private String endpoint;        // browser push endpoint
    private String p256dhKey;       // base64
    private String authSecret;      // base64
    private String userAgent;       // 識別瀏覽器
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiredAt;
}

// NotificationLogPO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogPO {
    private String logId;           // UUID
    private String userId;
    private String alertId;         // 可空（測試推播時 null）
    private NotifyChannel channel;
    private NotifyStatus status;
    private String payload;         // JSON（含內容、深層連結）
    private String externalMessageId; // SES messageId / Web Push response
    private String errorMessage;
    private LocalDateTime sentAt;
}

// NotificationDispatcher interface（給 stock-alert）
public interface NotificationDispatcher {
    /**
     * 分派推播：依使用者偏好決定 Web Push / Email 啟用，並處理降級。
     */
    void dispatch(NotificationRequest request);
}
```

### 5.3 對外 API

| Method | Path | Auth | Body | 說明 |
|--------|------|------|------|------|
| POST | `/api/v1/notify/subscribe` | ✅ | `{endpoint, p256dhKey, authSecret, userAgent}` | 註冊 Web Push subscription |
| POST | `/api/v1/notify/unsubscribe` | ✅ | `{subscriptionId}` | 取消訂閱 |
| POST | `/api/v1/notify/test` | ✅ | `{channel}` | Settings 測試推播 |

### 5.4 與 stock-alert 整合

- `stock-alert.AlertTriggeredEventListener` 透過 `@EventListener` 接收 `AlertTriggeredEvent`
- 呼叫 `NotificationDispatcher.dispatch(...)`
- 採 Spring Event（同步 + `@Async` 異步皆可，Wave 3 建議 **同步 + try/catch + audit log**，避免吞掉錯誤）

---

## 6. stock-boot（聚合與 Spring Security 主配置）

### 6.1 新增檔案

```
stock-boot/
└── src/main/java/tw/com/stockplatform/boot/
    ├── config/
    │   ├── SecurityConfig.java                         ✨新增（Spring Security FilterChain）
    │   └── CorsConfig.java                             ✨新增（CORS 配置）
    └── exception/
        └── GlobalExceptionHandler.java                 (既有, Wave 3 擴充新例外 mapping)
```

### 6.2 SecurityConfig 骨架

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authEntryPoint)        // → 200 + code 3001
                .accessDeniedHandler(accessDeniedHandler))        // → 200 + code 3002
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login",
                                 "/api/v1/auth/register",
                                 "/api/v1/auth/refresh",
                                 "/api/v1/search/stock",
                                 "/api/v1/search/hot",
                                 "/api/v1/quote/**",
                                 "/api/v1/fundamental/**",
                                 "/api/v1/chip/**",
                                 "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## 7. ArchUnit 測試（守護模組邊界）

放於 `stock-boot/src/test/java/.../arch/`：

```java
@AnalyzeClasses(packages = "tw.com.stockplatform")
class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule wave2_modules_should_not_depend_on_wave3 =
        noClasses().that().resideInAnyPackage("..quote..", "..fundamental..", "..chip..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..watchlist..", "..search..", "..alert..", "..notify..");

    @ArchTest
    static final ArchRule no_cycles =
        slices().matching("tw.com.stockplatform.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllers_in_correct_package =
        classes().that().areAnnotatedWith(RestController.class)
            .should().resideInAnyPackage("..controller..");
}
```

---

## 8. pom.xml 變更摘要

### 8.1 parent pom（`backend/pom.xml`）

新增 module（保留既有順序）：

```xml
<modules>
    <!-- 既有 14 個 modules -->
    <module>stock-search</module>     <!-- ✨新增 -->
    <module>stock-alert</module>      <!-- ✨新增 -->
</modules>
```

新增依賴版本管理：

```xml
<properties>
    <web-push.version>5.1.1</web-push.version>
    <aws-sdk-ses.version>2.28.16</aws-sdk-ses.version>
    <spring-security.version>6.3.4</spring-security.version> <!-- 由 spring-boot-parent 帶入 -->
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>nl.martijndwars</groupId>
            <artifactId>web-push</artifactId>
            <version>${web-push.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>ses</artifactId>
            <version>${aws-sdk-ses.version}</version>
        </dependency>
        <!-- 內部 module: stock-search / stock-alert -->
    </dependencies>
</dependencyManagement>
```

### 8.2 stock-boot 新增 dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>tw.com.stockplatform</groupId>
    <artifactId>stock-search</artifactId>
</dependency>
<dependency>
    <groupId>tw.com.stockplatform</groupId>
    <artifactId>stock-alert</artifactId>
</dependency>
```

---

## 9. 給 Felix（前端）的對應

對應前端套件路徑（資訊性，非 Preston 強制）：

```
frontend/src/
├── pages/
│   ├── Home/                       (新：自選股彙整 + 熱門搜尋)
│   ├── Watchlist/                  (新)
│   ├── Alert/                      (新：警示管理頁)
│   └── Settings/Notification/      (新：推播偏好)
├── components/
│   ├── SearchBox/                  (新：debounce + 自動完成)
│   └── WatchlistButton/            (新：★ 加入自選按鈕)
├── services/
│   ├── searchService.ts            (新)
│   ├── watchlistService.ts         (新)
│   ├── alertService.ts             (新)
│   └── notifyService.ts            (新)
└── sw/
    └── push-sw.ts                  (新：Service Worker for Web Push)
```

---

## 10. 變更歷史

| 日期 | 版本 | 變更 | 作者 |
|------|------|------|------|
| 2026-04-23 | v1.0 | 初版 | Preston |

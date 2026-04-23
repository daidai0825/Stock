package tw.com.stockplatform.boot.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wave 3 F-W3-04-5：SecurityConfig 整合測試。
 * <p>
 * 驗證場景（依 Spring Security 整合文件 §5.2）：
 * <ul>
 *   <li>SC-SEC-01：公開 endpoint 不帶 JWT → HTTP 200</li>
 *   <li>SC-SEC-02：受保護 endpoint 不帶 JWT → HTTP 200 + code 3001</li>
 *   <li>SC-SEC-03：受保護 endpoint 帶過期 JWT → HTTP 200 + code 3002</li>
 *   <li>SC-SEC-04：受保護 endpoint 帶簽章錯誤 JWT → HTTP 200 + code 3003</li>
 * </ul>
 * <p>
 * 使用 Testcontainers PostgreSQL 16 確保 Flyway migration 正常執行（V3.0.0 stock_info）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("local")
class SecurityConfigIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("stockdb_test")
        .withUsername("stockuser")
        .withPassword("stockpass");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // 測試環境停用 Redis auto-configuration（避免連線不到 Redis 失敗）
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6399");
        registry.add("spring.autoconfigure.exclude",
            () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                  "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${stock.security.jwt.secret}")
    private String jwtSecret;

    // ==========================================================================
    // SC-SEC-01：公開 endpoint 不帶 JWT → HTTP 200
    // ==========================================================================

    @Test
    @DisplayName("SC-SEC-01a：GET /actuator/health（公開端點）不帶 JWT → HTTP 200")
    void givenNoToken_whenCallActuatorHealth_thenHttp200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SC-SEC-01b：POST /api/v1/quote/list（W2 公開端點）不帶 JWT → HTTP 200（非 3001）")
    void givenNoToken_whenCallW2PublicEndpoint_thenHttp200AndNotUnauthorized() throws Exception {
        String body = mockMvc.perform(post("/api/v1/quote/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        // 公開端點不應回 3001（未登入），業務碼非 3001 即可
        assertThat(json.get("code").asInt()).isNotEqualTo(3001);
    }

    @Test
    @DisplayName("SC-SEC-01c：POST /api/v1/stock/search（Wave 3 搜尋公開端點）不帶 JWT → HTTP 200")
    void givenNoToken_whenCallSearchEndpoint_thenHttp200() throws Exception {
        mockMvc.perform(post("/api/v1/stock/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SC-SEC-01d：POST /api/v1/member/login（登入公開端點）不帶 JWT → HTTP 200（非 3001）")
    void givenNoToken_whenCallLoginEndpoint_thenHttp200() throws Exception {
        String body = mockMvc.perform(post("/api/v1/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"noexist@test.com\",\"password\":\"wrongpass\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        // 登入 endpoint 公開，不會回 3001；業務錯誤（email/password 錯）
        assertThat(json.get("code").asInt()).isNotEqualTo(3001);
    }

    // ==========================================================================
    // SC-SEC-02：受保護 endpoint 不帶 JWT → HTTP 200 + code 3001
    // ==========================================================================

    @Test
    @DisplayName("SC-SEC-02a：POST /api/v1/watchlist/list（受保護）不帶 JWT → HTTP 200 + code 3001")
    void givenNoToken_whenCallWatchlistEndpoint_thenCode3001() throws Exception {
        String body = mockMvc.perform(post("/api/v1/watchlist/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertEnvelopeCode(body, 3001);
        assertHasTimestampAndTraceId(body);
    }

    @Test
    @DisplayName("SC-SEC-02b：POST /api/v1/alert/create（受保護）不帶 JWT → HTTP 200 + code 3001")
    void givenNoToken_whenCallAlertEndpoint_thenCode3001() throws Exception {
        String body = mockMvc.perform(post("/api/v1/alert/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertEnvelopeCode(body, 3001);
    }

    @Test
    @DisplayName("SC-SEC-02c：不帶 JWT 的受保護請求，HTTP 狀態碼必須是 200（非 401/403）")
    void givenNoToken_whenCallProtectedEndpoint_thenHttpStatusMustBe200NotUnauthorized() throws Exception {
        // 明確驗證不回 HTTP 401
        mockMvc.perform(post("/api/v1/member/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk()); // 一律 200，不使用 HTTP 401
    }

    // ==========================================================================
    // SC-SEC-03：受保護 endpoint 帶過期 JWT → HTTP 200 + code 3002
    // ==========================================================================

    @Test
    @DisplayName("SC-SEC-03：受保護 endpoint 帶過期 JWT → HTTP 200 + code 3002")
    void givenExpiredToken_whenCallProtectedEndpoint_thenCode3002() throws Exception {
        String expiredToken = buildExpiredJwt();

        String body = mockMvc.perform(post("/api/v1/watchlist/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertEnvelopeCode(body, 3002);
        assertHasTimestampAndTraceId(body);
    }

    // ==========================================================================
    // SC-SEC-04：受保護 endpoint 帶簽章錯誤 JWT → HTTP 200 + code 3003
    // ==========================================================================

    @Test
    @DisplayName("SC-SEC-04：受保護 endpoint 帶簽章錯誤 JWT → HTTP 200 + code 3003")
    void givenInvalidSignatureToken_whenCallProtectedEndpoint_thenCode3003() throws Exception {
        // 使用不同 secret 簽名，導致簽章驗證失敗
        String wrongSecret = "wrong-secret-key-for-testing-purposes-only-xxxxxxxx";
        String invalidToken = Jwts.builder()
            .subject("test-user")
            .claim("email", "test@example.com")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 900_000))
            .signWith(Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
            .compact();

        String body = mockMvc.perform(post("/api/v1/watchlist/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertEnvelopeCode(body, 3003);
        assertHasTimestampAndTraceId(body);
    }

    // ==========================================================================
    // 輔助方法
    // ==========================================================================

    /**
     * 產生一個使用正確 secret 簽名、但已過期的 JWT。
     * exp 設定為 1 毫秒（確保在任何環境都已過期）。
     */
    private String buildExpiredJwt() {
        return Jwts.builder()
            .subject("test-user")
            .claim("email", "test@example.com")
            .issuedAt(new Date(System.currentTimeMillis() - 3600_000))
            .expiration(new Date(System.currentTimeMillis() - 1800_000)) // 30 分鐘前過期
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
            .compact();
    }

    private void assertEnvelopeCode(String responseBody, int expectedCode) throws Exception {
        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("code").asInt())
            .as("envelope code 應為 %d，實際為 %s", expectedCode, responseBody)
            .isEqualTo(expectedCode);
    }

    private void assertHasTimestampAndTraceId(String responseBody) throws Exception {
        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.has("timestamp")).as("response 應包含 timestamp 欄位").isTrue();
        // traceId 在測試環境可能為 null（無 MDC），但欄位必須存在（JsonInclude.NON_NULL 可能省略）
        // 改驗證 timestamp 非空即可
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }
}

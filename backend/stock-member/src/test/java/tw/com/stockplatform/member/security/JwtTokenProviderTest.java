package tw.com.stockplatform.member.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tw.com.stockplatform.member.security.exception.JwtExpiredException;
import tw.com.stockplatform.member.security.exception.JwtInvalidException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 例外分流測試（TD-2 驗證）。
 * <p>
 * 確認 parse() 已包裝為自家例外，Controller 不再直接依賴 jjwt API：
 * <ul>
 *   <li>過期 token → {@link JwtExpiredException}（→ 3002 TOKEN_EXPIRED）</li>
 *   <li>簽章不符   → {@link JwtInvalidException}（→ 3003 TOKEN_INVALID）</li>
 *   <li>格式錯誤   → {@link JwtInvalidException}（→ 3003 TOKEN_INVALID）</li>
 * </ul>
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-must-be-at-least-32-chars-long";
    private static final String OTHER_SECRET = "another-secret-for-signature-mismatch-32+ch";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", SECRET);
        ReflectionTestUtils.setField(provider, "accessTokenTtlSeconds", 900L);
        provider.init();
    }

    @Test
    @DisplayName("parse：合法 token 取得 subject = userId")
    void parse_Valid() {
        String token = provider.createAccessToken("uid-123", "test@example.com");

        assertThat(provider.parse(token).getSubject()).isEqualTo("uid-123");
    }

    @Test
    @DisplayName("parse：過期 token 拋 JwtExpiredException（→ controller 應回 3002 TOKEN_EXPIRED）")
    void parse_Expired() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date past = new Date(System.currentTimeMillis() - 60_000L);
        String expired = Jwts.builder()
            .subject("uid-123")
            .issuedAt(new Date(past.getTime() - 60_000L))
            .expiration(past)
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertThatThrownBy(() -> provider.parse(expired))
            .isInstanceOf(JwtExpiredException.class);
    }

    @Test
    @DisplayName("parse：簽章不符的 token 拋 JwtInvalidException（→ controller 應回 3003 TOKEN_INVALID）")
    void parse_InvalidSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String tampered = Jwts.builder()
            .subject("uid-evil")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(otherKey, Jwts.SIG.HS256)
            .compact();

        assertThatThrownBy(() -> provider.parse(tampered))
            .isInstanceOf(JwtInvalidException.class);
    }

    @Test
    @DisplayName("parse：格式錯誤的 token 拋 JwtInvalidException（→ controller 應回 3003 TOKEN_INVALID）")
    void parse_Malformed() {
        assertThatThrownBy(() -> provider.parse("not-a-jwt-at-all"))
            .isInstanceOf(JwtInvalidException.class);
    }
}

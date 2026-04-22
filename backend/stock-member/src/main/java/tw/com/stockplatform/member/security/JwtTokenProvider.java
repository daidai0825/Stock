package tw.com.stockplatform.member.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tw.com.stockplatform.common.util.TimeUtils;
import tw.com.stockplatform.member.security.exception.JwtExpiredException;
import tw.com.stockplatform.member.security.exception.JwtInvalidException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Access Token 簽發與驗證。
 * Refresh Token 留至 Wave 2（含 Redis 黑名單）。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${stock.security.jwt.secret}")
    private String secret;

    @Value("${stock.security.jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("stock.security.jwt.secret 至少需要 32 字元（HS256）");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId, String email) {
        Date now = Date.from(TimeUtils.now().atZone(TimeUtils.TAIPEI).toInstant());
        Date expiry = Date.from(TimeUtils.now().plusSeconds(accessTokenTtlSeconds)
            .atZone(TimeUtils.TAIPEI).toInstant());

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * 解析 JWT，失敗時一律包裝為自家例外，遮蔽 jjwt 實作細節。
     *
     * @throws JwtExpiredException  Token 已過期（→ 3002）
     * @throws JwtInvalidException  簽章不符或格式錯誤（→ 3003）
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new JwtExpiredException(ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtInvalidException(ex);
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}

package tw.com.stockplatform.boot.config.security;

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
 * Wave 3 F-W3-04-2：JWT Access Token 驗證 Filter。
 * <p>
 * 解析 Authorization: Bearer xxx，建立 Spring Security Authentication。
 * <p>
 * 解析失敗時不直接寫 response，而是設定 request attribute 後繼續 filter chain，
 * 由 {@link ApiAuthenticationEntryPoint} 依 attribute 判斷回對應錯誤碼：
 * <ul>
 *   <li>TOKEN_EXPIRED → code 3002</li>
 *   <li>TOKEN_INVALID → code 3003</li>
 *   <li>未帶 token / 其他 → code 3001</li>
 * </ul>
 * 公開端點（permitAll）帶 JWT 仍會嘗試解析，成功時注入 SecurityContext（用於區分登入狀態）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String JWT_ERROR_ATTR = "jwtAuthError";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

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
            log.debug("JWT token expired for request: {}", request.getRequestURI());
            request.setAttribute(JWT_ERROR_ATTR, "TOKEN_EXPIRED");
        } catch (JwtInvalidException ex) {
            log.warn("JWT token invalid for request: {}", request.getRequestURI());
            request.setAttribute(JWT_ERROR_ATTR, "TOKEN_INVALID");
        }
        chain.doFilter(request, response);
    }
}

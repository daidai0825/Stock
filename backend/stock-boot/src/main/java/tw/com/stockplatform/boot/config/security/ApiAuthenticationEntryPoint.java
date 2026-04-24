package tw.com.stockplatform.boot.config.security;

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
 * Wave 3 F-W3-04-1 / D-08 拍板：未授權一律 HTTP 200 + envelope code 3001/3002/3003。
 * <p>
 * 取代 Spring Security 預設行為（HTTP 401 + WWW-Authenticate header）。
 * <p>
 * 場景對應：
 * <ul>
 *   <li>未帶 token → 3001 UNAUTHORIZED（未登入）</li>
 *   <li>token 已過期 → 3002 TOKEN_EXPIRED</li>
 *   <li>token 簽章錯誤 / 格式無效 → 3003 TOKEN_INVALID</li>
 * </ul>
 * <p>
 * 滲透測試說明（W6）：本設計符合平台 api-design.md Envelope Pattern，非漏洞。
 * 對應決策紀錄 D-2026-04-23-08。
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode errorCode = resolveErrorCode(request);
        ApiResponse<Void> body = ApiResponse.fail(errorCode);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        String jwtError = (String) request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTR);
        if ("TOKEN_EXPIRED".equals(jwtError)) {
            return ErrorCode.TOKEN_EXPIRED;
        }
        if ("TOKEN_INVALID".equals(jwtError)) {
            return ErrorCode.TOKEN_INVALID;
        }
        return ErrorCode.UNAUTHORIZED;
    }
}

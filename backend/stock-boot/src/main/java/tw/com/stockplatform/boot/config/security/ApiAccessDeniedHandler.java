package tw.com.stockplatform.boot.config.security;

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
 * Wave 3 F-W3-04：已登入但權限不足時的 Handler。
 * <p>
 * 取代 Spring Security 預設行為（HTTP 403），回 HTTP 200 + envelope code 3004 FORBIDDEN。
 * <p>
 * Wave 3 暫無 RBAC（單一使用者角色），預留未來 Admin 角色使用。
 * IDOR 情境（UserA 存取 UserB 的資源）由 Service 層拋出 BusinessException(FORBIDDEN) 處理，
 * 非走此 Handler。
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.FORBIDDEN);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}

package tw.com.stockplatform.member.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tw.com.stockplatform.common.response.ApiResponse;
import tw.com.stockplatform.member.security.exception.JwtAuthException;

/**
 * M-MEMBER 模組例外處理器。
 * <p>
 * M-BE-W2-10: 將 JwtAuthException 的處理提升到 ControllerAdvice，
 * MemberController.currentUserId() 移除 try-catch，統一由此攔截。
 * <p>
 * 架構說明：JwtAuthException 定義於 stock-member，不應污染 stock-common 的
 * GlobalExceptionHandler（避免循環依賴）。此 Handler 優先順序高於 GlobalExceptionHandler，
 * 能正確攔截 JwtAuthException 子類（JwtExpiredException、JwtInvalidException）。
 */
@Slf4j
@RestControllerAdvice(basePackages = "tw.com.stockplatform.member")
public class MemberExceptionHandler {

    /**
     * 攔截 JWT 驗證失敗例外，統一回 HTTP 200 + 對應業務碼。
     * <ul>
     *   <li>{@link tw.com.stockplatform.member.security.exception.JwtExpiredException} → 3002</li>
     *   <li>{@link tw.com.stockplatform.member.security.exception.JwtInvalidException} → 3003</li>
     * </ul>
     */
    @ExceptionHandler(JwtAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtAuth(JwtAuthException ex) {
        log.warn("JwtAuthException: code={} message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ex.getErrorCode()));
    }
}

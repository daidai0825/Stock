package tw.com.stockplatform.member.security.exception;

import tw.com.stockplatform.common.constant.ErrorCode;

/**
 * JWT 驗證失敗的基底例外，對外遮蔽 jjwt 實作細節。
 * <p>
 * 子類別依失效原因分流：
 * <ul>
 *   <li>{@link JwtExpiredException} → 3002 TOKEN_EXPIRED</li>
 *   <li>{@link JwtInvalidException} → 3003 TOKEN_INVALID</li>
 * </ul>
 */
public abstract sealed class JwtAuthException extends RuntimeException
    permits JwtExpiredException, JwtInvalidException {

    private final ErrorCode errorCode;

    protected JwtAuthException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

package tw.com.stockplatform.member.security.exception;

import tw.com.stockplatform.common.constant.ErrorCode;

/**
 * Token 已過期 → 3002 TOKEN_EXPIRED。
 */
public final class JwtExpiredException extends JwtAuthException {

    public JwtExpiredException(Throwable cause) {
        super(ErrorCode.TOKEN_EXPIRED, ErrorCode.TOKEN_EXPIRED.getMessage(), cause);
    }
}

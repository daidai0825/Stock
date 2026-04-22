package tw.com.stockplatform.member.security.exception;

import tw.com.stockplatform.common.constant.ErrorCode;

/**
 * Token 簽章不符或格式錯誤 → 3003 TOKEN_INVALID。
 */
public final class JwtInvalidException extends JwtAuthException {

    public JwtInvalidException(Throwable cause) {
        super(ErrorCode.TOKEN_INVALID, ErrorCode.TOKEN_INVALID.getMessage(), cause);
    }
}

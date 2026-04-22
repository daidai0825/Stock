package tw.com.stockplatform.common.exception;

import lombok.Getter;
import tw.com.stockplatform.common.constant.ErrorCode;

/**
 * 外部服務呼叫失敗例外（5xxx 段）。
 * <p>
 * 由 TWSE / OTC / MOPS Client 拋出，{@code GlobalExceptionHandler} 統一攔截轉為 HTTP 200 + 業務碼。
 */
@Getter
public class ExternalServiceException extends RuntimeException {

    private final int code;

    public ExternalServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public ExternalServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}

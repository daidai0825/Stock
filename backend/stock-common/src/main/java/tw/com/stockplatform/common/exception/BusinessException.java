package tw.com.stockplatform.common.exception;

import lombok.Getter;
import tw.com.stockplatform.common.constant.ErrorCode;

/**
 * 業務例外，由 {@code GlobalExceptionHandler} 攔截轉為 HTTP 200 + 業務碼。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

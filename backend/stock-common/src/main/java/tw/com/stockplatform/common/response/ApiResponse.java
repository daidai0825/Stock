package tw.com.stockplatform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.MDC;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.util.TimeUtils;

import java.util.List;

/**
 * 統一 API Envelope。
 * <p>
 * 所有 REST API 一律 HTTP 200，業務狀態以 {@code code} 表達。
 * 成功時 code = 0；錯誤時為 {@link ErrorCode#getCode()}。
 */
@Getter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final List<FieldError> errors;
    private final String timestamp;
    private final String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .data(data)
            .timestamp(TimeUtils.nowIso8601())
            .traceId(MDC.get("traceId"))
            .build();
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return ApiResponse.<T>builder()
            .code(code)
            .message(message)
            .timestamp(TimeUtils.nowIso8601())
            .traceId(MDC.get("traceId"))
            .build();
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> ApiResponse<T> fail(int code, String message, List<FieldError> errors) {
        return ApiResponse.<T>builder()
            .code(code)
            .message(message)
            .errors(errors)
            .timestamp(TimeUtils.nowIso8601())
            .traceId(MDC.get("traceId"))
            .build();
    }

    /**
     * 詳細欄位錯誤（用於參數校驗）。
     */
    @Getter
    @Builder
    @ToString
    public static class FieldError {
        private final String field;
        private final String message;
    }
}

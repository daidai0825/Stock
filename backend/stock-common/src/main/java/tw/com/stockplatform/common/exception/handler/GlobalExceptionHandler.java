package tw.com.stockplatform.common.exception.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.exception.ExternalServiceException;
import tw.com.stockplatform.common.response.ApiResponse;

import java.util.List;

/**
 * 全域例外處理：所有業務例外回 HTTP 200 + 業務碼。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("BusinessException code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(ExternalServiceException ex) {
        log.error("ExternalServiceException code={}, message={}", ex.getCode(), ex.getMessage(), ex.getCause());
        return ResponseEntity.ok(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldError)
            .toList();
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.ok(
            ApiResponse.fail(ErrorCode.PARAM_FORMAT_INVALID.getCode(),
                ErrorCode.PARAM_FORMAT_INVALID.getMessage(),
                fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ErrorCode.PARAM_FORMAT_INVALID));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegal(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ErrorCode.PARAM_FORMAT_INVALID.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.OK)
            .body(ApiResponse.fail(ErrorCode.UNKNOWN_ERROR));
    }

    private ApiResponse.FieldError toFieldError(FieldError fieldError) {
        return ApiResponse.FieldError.builder()
            .field(fieldError.getField())
            .message(fieldError.getDefaultMessage())
            .build();
    }
}

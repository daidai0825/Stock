package tw.com.stockplatform.alert.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.response.ApiResponse;

/**
 * Wave 3 stock-alert placeholder Controller。
 * <p>
 * 業務邏輯實作於 W3 W3-W4 開發週期。
 * W1 目的：確認模組骨架可編譯，受保護端點被 SecurityConfig 正確攔截（authenticated()）。
 */
@RestController
@RequestMapping("/api/v1/alert")
public class AlertPlaceholderController {

    @PostMapping("/create")
    public ApiResponse<Void> create() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE);
    }

    @PostMapping("/list")
    public ApiResponse<Void> list() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE);
    }

    @PostMapping("/update-status")
    public ApiResponse<Void> updateStatus() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE);
    }
}

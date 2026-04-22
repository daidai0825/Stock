package tw.com.stockplatform.technical.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.response.ApiResponse;

/**
 * Wave 1 placeholder：Technical 模組業務尚未實作。
 * 任何呼叫一律回 9003 FEATURE_NOT_AVAILABLE，提示功能尚未開放。
 * Wave 2+ 由各 Squad 接手實作。
 */
@RestController
@RequestMapping("/api/v1/technical")
public class TechnicalPlaceholderController {

    @PostMapping("/_status")
    public ApiResponse<String> status() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE.getCode(),
            "功能尚未開放，敬請期待");
    }
}

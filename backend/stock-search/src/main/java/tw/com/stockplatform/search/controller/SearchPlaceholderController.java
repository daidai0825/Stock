package tw.com.stockplatform.search.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.response.ApiResponse;

/**
 * Wave 3 stock-search placeholder Controller。
 * <p>
 * 業務邏輯實作於 W3 W2-W3 開發週期。
 * W1 目的：確認模組骨架可編譯並與 SecurityConfig 公開端點正確對應。
 */
@RestController
@RequestMapping("/api/v1/stock")
public class SearchPlaceholderController {

    @PostMapping("/search")
    public ApiResponse<Void> search() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE);
    }

    @PostMapping("/hot-search")
    public ApiResponse<Void> hotSearch() {
        return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE);
    }
}

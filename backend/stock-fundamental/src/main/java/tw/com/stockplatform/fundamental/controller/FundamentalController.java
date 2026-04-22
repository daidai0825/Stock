package tw.com.stockplatform.fundamental.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.common.audit.Audited;
import tw.com.stockplatform.common.response.ApiResponse;
import tw.com.stockplatform.fundamental.dto.request.FundamentalGetRequest;
import tw.com.stockplatform.fundamental.dto.response.FundamentalDTO;
import tw.com.stockplatform.fundamental.service.FundamentalService;

/**
 * M-FUND 基本面 REST API。
 */
@RestController
@RequestMapping("/api/v1/fundamental")
@RequiredArgsConstructor
public class FundamentalController {

    private final FundamentalService fundamentalService;

    /**
     * 查詢個股 EPS / PER / PBR / ROE。
     */
    @PostMapping("/get")
    @Audited(action = "FUNDAMENTAL_GET", target = "stock")
    public ApiResponse<FundamentalDTO> getFundamental(@Valid @RequestBody FundamentalGetRequest request) {
        return ApiResponse.success(fundamentalService.getFundamental(request));
    }
}

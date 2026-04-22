package tw.com.stockplatform.quote.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.common.audit.Audited;
import tw.com.stockplatform.common.response.ApiResponse;
import tw.com.stockplatform.quote.dto.request.QuoteGetRequest;
import tw.com.stockplatform.quote.dto.request.QuoteHistoryRequest;
import tw.com.stockplatform.quote.dto.request.QuoteListRequest;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;
import tw.com.stockplatform.quote.dto.response.QuoteHistoryResponse;
import tw.com.stockplatform.quote.service.QuoteService;

import java.util.List;

/**
 * M-QUOTE 行情 REST API（schema-lock v1.0 對齊）。
 * <p>
 * 統一 Envelope Pattern（HTTP 200 + 業務碼）。
 * 快取策略由 {@link tw.com.stockplatform.quote.service.impl.QuoteServiceImpl} 控制。
 */
@RestController
@RequestMapping("/api/v1/quote")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    /**
     * POST /api/v1/quote/get — 查詢單一股票最新行情。
     */
    @PostMapping("/get")
    @Audited(action = "QUOTE_GET", target = "stock")
    public ApiResponse<QuoteDTO> getQuote(@Valid @RequestBody QuoteGetRequest request) {
        return ApiResponse.success(quoteService.getQuote(request));
    }

    /**
     * POST /api/v1/quote/list — 批次查詢多檔股票最新行情。
     */
    @PostMapping("/list")
    @Audited(action = "QUOTE_LIST", target = "stock")
    public ApiResponse<List<QuoteDTO>> listQuotes(@Valid @RequestBody QuoteListRequest request) {
        return ApiResponse.success(quoteService.listQuotes(request));
    }

    /**
     * POST /api/v1/quote/history — 查詢個股 K 線歷史行情（daily/weekly/monthly）。
     */
    @PostMapping("/history")
    @Audited(action = "QUOTE_HISTORY", target = "stock")
    public ApiResponse<QuoteHistoryResponse> getHistory(@Valid @RequestBody QuoteHistoryRequest request) {
        return ApiResponse.success(quoteService.getHistory(request));
    }
}

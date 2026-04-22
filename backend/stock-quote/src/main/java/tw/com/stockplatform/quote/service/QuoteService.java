package tw.com.stockplatform.quote.service;

import tw.com.stockplatform.quote.dto.request.QuoteGetRequest;
import tw.com.stockplatform.quote.dto.request.QuoteHistoryRequest;
import tw.com.stockplatform.quote.dto.request.QuoteListRequest;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;
import tw.com.stockplatform.quote.dto.response.QuoteHistoryResponse;

import java.util.List;

/**
 * M-QUOTE 行情服務介面（schema-lock v1.0 對齊）。
 */
public interface QuoteService {

    /**
     * 查詢單一股票最新行情（含 Redis 快取）。
     */
    QuoteDTO getQuote(QuoteGetRequest request);

    /**
     * 批次查詢多檔股票最新行情（含 Redis 快取）。
     */
    List<QuoteDTO> listQuotes(QuoteListRequest request);

    /**
     * 查詢個股 K 線歷史行情（含 Redis 快取）。
     * 支援 daily / weekly / monthly 週期，weekly/monthly 於 Service 層聚合。
     */
    QuoteHistoryResponse getHistory(QuoteHistoryRequest request);
}

package tw.com.stockplatform.quote.service;

import tw.com.stockplatform.quote.dto.request.QuoteGetRequest;
import tw.com.stockplatform.quote.dto.request.QuoteHistoryRequest;
import tw.com.stockplatform.quote.dto.request.QuoteListRequest;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;

import java.util.List;

/**
 * M-QUOTE 行情服務介面。
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
     * 查詢個股月歷史行情（含 Redis 快取）。
     */
    List<QuoteDTO> getHistory(QuoteHistoryRequest request);
}

package tw.com.stockplatform.quote.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批次查詢多檔股票最新行情請求。
 */
public record QuoteListRequest(
    @NotNull(message = "股票代號清單不得為空")
    @Size(min = 1, max = 50, message = "一次最多查詢 50 檔")
    List<String> stockIds,

    /**
     * 市場別篩選，null 表示不限。
     * 合法值：TWSE、OTC
     */
    String market
) {}

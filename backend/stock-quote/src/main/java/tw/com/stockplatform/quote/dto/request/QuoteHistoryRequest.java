package tw.com.stockplatform.quote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 查詢個股 OHLC 歷史行情請求。
 */
public record QuoteHistoryRequest(
    @NotBlank(message = "股票代號不得為空")
    String stockId,

    @NotNull(message = "查詢月份不得為空")
    LocalDate month
) {}

package tw.com.stockplatform.quote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 查詢單一股票最新行情請求。
 */
public record QuoteGetRequest(
    @NotBlank(message = "股票代號不得為空")
    @Size(max = 10, message = "股票代號長度不得超過 10 字元")
    String stockId
) {}

package tw.com.stockplatform.fundamental.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 查詢個股基本面請求（EPS / PER / PBR / ROE）。
 */
public record FundamentalGetRequest(
    @NotBlank(message = "股票代號不得為空")
    @Size(max = 10, message = "股票代號長度不得超過 10 字元")
    String stockId
) {}

package tw.com.stockplatform.chip.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 查詢個股三大法人籌碼請求。
 */
public record ChipGetRequest(
    @NotBlank(message = "股票代號不得為空")
    @Size(max = 10, message = "股票代號長度不得超過 10 字元")
    String stockId
) {}

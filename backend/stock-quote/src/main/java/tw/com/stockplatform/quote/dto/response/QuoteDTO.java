package tw.com.stockplatform.quote.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 單一股票行情 DTO。
 * <p>
 * {@code isStale} = true 表示外部資料源無法取得最新行情（週末/假日/收盤前），
 * 回傳的是 DB 最後一筆有效交易日資料，前端應在 UI 標示「非即時」。
 * <p>
 * Round 2 TODO: 等 Peter SRS lock 後對齊 price/change/changePercent 欄位命名。
 */
public record QuoteDTO(
    String stockId,
    String stockName,
    String market,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    Long volume,
    LocalDate quoteDate,
    boolean isStale
) {}

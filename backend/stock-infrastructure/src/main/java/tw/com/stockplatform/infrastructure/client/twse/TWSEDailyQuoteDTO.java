package tw.com.stockplatform.infrastructure.client.twse;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TWSE 每日收盤行情（單筆股票）。
 */
public record TWSEDailyQuoteDTO(
    String stockId,
    String stockName,
    LocalDate tradeDate,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    Long volume
) {}

package tw.com.stockplatform.infrastructure.client.twse;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TWSE 個股 OHLC 歷史（單日）。
 */
public record TWSEOhlcDTO(
    String stockId,
    LocalDate tradeDate,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    Long volume,
    Long turnover
) {}

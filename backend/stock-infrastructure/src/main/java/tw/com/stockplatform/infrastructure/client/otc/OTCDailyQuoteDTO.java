package tw.com.stockplatform.infrastructure.client.otc;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OTC（上櫃）每日收盤行情（單筆股票）。
 */
public record OTCDailyQuoteDTO(
    String stockId,
    String stockName,
    LocalDate tradeDate,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    Long volume
) {}

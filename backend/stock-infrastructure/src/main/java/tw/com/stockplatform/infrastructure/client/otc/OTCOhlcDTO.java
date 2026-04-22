package tw.com.stockplatform.infrastructure.client.otc;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OTC（上櫃）個股 OHLC 歷史（單日）。
 */
public record OTCOhlcDTO(
    String stockId,
    LocalDate tradeDate,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    Long volume
) {}

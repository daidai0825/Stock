package tw.com.stockplatform.infrastructure.client.twse;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 三大法人買賣超（上市，TWSE）。
 */
public record TWSEInstitutionalDTO(
    String stockId,
    String stockName,
    LocalDate tradeDate,
    BigDecimal foreignBuyShares,
    BigDecimal foreignSellShares,
    BigDecimal foreignNetShares,
    BigDecimal investmentTrustBuyShares,
    BigDecimal investmentTrustSellShares,
    BigDecimal investmentTrustNetShares,
    BigDecimal dealerNetShares
) {}

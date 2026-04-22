package tw.com.stockplatform.chip.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 三大法人籌碼 DTO（單日）。
 */
public record ChipDTO(
    String stockId,
    String stockName,
    String market,
    LocalDate tradeDate,
    BigDecimal foreignNetShares,
    BigDecimal investmentTrustNetShares,
    BigDecimal dealerNetShares,
    BigDecimal totalInstitutionalNet
) {}

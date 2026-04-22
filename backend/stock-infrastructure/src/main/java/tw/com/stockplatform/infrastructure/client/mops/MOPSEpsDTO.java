package tw.com.stockplatform.infrastructure.client.mops;

import java.math.BigDecimal;

/**
 * MOPS 個股 EPS（單季）。
 */
public record MOPSEpsDTO(
    String stockId,
    int year,
    int quarter,
    BigDecimal eps
) {}

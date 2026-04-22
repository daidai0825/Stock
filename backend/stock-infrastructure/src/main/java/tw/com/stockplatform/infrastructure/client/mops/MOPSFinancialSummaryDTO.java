package tw.com.stockplatform.infrastructure.client.mops;

import java.math.BigDecimal;

/**
 * MOPS 個股財務摘要（PER / PBR / ROE）。
 * <p>
 * perRatio：本益比（P/E Ratio）
 * pbrRatio：股價淨值比（P/B Ratio）
 * roe：股東權益報酬率（%）
 */
public record MOPSFinancialSummaryDTO(
    String stockId,
    BigDecimal perRatio,
    BigDecimal pbrRatio,
    BigDecimal roe
) {}

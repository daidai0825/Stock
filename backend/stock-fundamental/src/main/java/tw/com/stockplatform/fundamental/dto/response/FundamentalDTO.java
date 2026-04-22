package tw.com.stockplatform.fundamental.dto.response;

import java.math.BigDecimal;

/**
 * 個股基本面資料 DTO。
 */
public record FundamentalDTO(
    String stockId,
    String stockName,
    BigDecimal eps,
    BigDecimal perRatio,
    BigDecimal pbrRatio,
    BigDecimal roe,
    int reportYear,
    int reportQuarter
) {}

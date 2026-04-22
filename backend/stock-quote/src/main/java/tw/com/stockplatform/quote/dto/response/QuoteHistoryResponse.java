package tw.com.stockplatform.quote.dto.response;

import tw.com.stockplatform.quote.enums.KLinePeriod;

import java.util.List;

/**
 * 個股 K 線歷史行情回應（schema-lock v1.0 §3.3）。
 * <p>
 * items 依 date 升冪排序。
 */
public record QuoteHistoryResponse(
    String stockId,
    KLinePeriod period,
    List<HistoryItem> items
) {}

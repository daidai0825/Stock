package tw.com.stockplatform.chip.dto.response;

/**
 * 單一法人買賣超資料（schema-lock v1.0 §5.3 InstitutionItem）。
 * <p>
 * {@code name} 固定為繁體中文：「外資」/「投信」/「自營商」。
 * {@code netBuySell} = buy − sell，正值買超，負值賣超。
 */
public record InstitutionItem(
    String name,
    long buy,
    long sell,
    long netBuySell
) {}

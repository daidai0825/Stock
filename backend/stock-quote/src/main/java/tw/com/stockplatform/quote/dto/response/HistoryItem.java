package tw.com.stockplatform.quote.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * K 線資料單筆（schema-lock v1.0 §3.3 HistoryItem）。
 * <p>
 * weekly/monthly 的聚合規則（後端計算）：
 * <ul>
 *   <li>date：週/月首個交易日</li>
 *   <li>open：週/月首個交易日 open</li>
 *   <li>high：週/月最高 high</li>
 *   <li>low：週/月最低 low</li>
 *   <li>close：週/月最後一個交易日 close</li>
 *   <li>volume：週/月成交量加總</li>
 * </ul>
 */
public record HistoryItem(
    LocalDate date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    Long volume
) {}

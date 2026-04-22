package tw.com.stockplatform.fundamental.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 個股基本面資料 DTO（schema-lock v1.0 §4.3）。
 * <p>
 * 仲裁 D2 對齊：
 * <ul>
 *   <li>{@code per}（原 perRatio，禁止使用 perRatio）</li>
 *   <li>{@code pbr}（原 pbrRatio，禁止使用 pbrRatio）</li>
 *   <li>{@code updatedAt}：資料最後更新時間（ISO 8601 + +08:00）</li>
 *   <li>{@code source}：固定為 "MOPS"</li>
 * </ul>
 */
public record FundamentalDTO(
    String stockId,
    String stockName,
    BigDecimal eps,
    BigDecimal per,
    BigDecimal pbr,
    BigDecimal roe,
    int reportYear,
    int reportQuarter,
    LocalDateTime updatedAt,
    String source
) {}

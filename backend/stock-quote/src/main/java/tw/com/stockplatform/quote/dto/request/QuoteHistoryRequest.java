package tw.com.stockplatform.quote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tw.com.stockplatform.quote.enums.KLinePeriod;

import java.time.LocalDate;

/**
 * 查詢個股 K 線歷史行情請求（schema-lock v1.0 §3.2）。
 * <p>
 * 仲裁 D3：廢除原 {@code month: LocalDate} 語意，改為 period enum + 可選日期範圍。
 * <p>
 * 預設範圍（若 startDate/endDate 未提供）：
 * <ul>
 *   <li>daily：請求日往前 90 個自然日</li>
 *   <li>weekly：請求日往前 1 年</li>
 *   <li>monthly：請求日往前 5 年</li>
 * </ul>
 * 最長查詢範圍：daily/weekly ≤ 5 年；monthly ≤ 10 年（超出回 1003）。
 */
public record QuoteHistoryRequest(
    @NotBlank(message = "股票代號不得為空")
    String stockId,

    @NotNull(message = "資料週期不得為空（daily / weekly / monthly）")
    KLinePeriod period,

    /** 起始日期（YYYY-MM-DD），null 時依 period 套用預設 */
    LocalDate startDate,

    /** 結束日期（YYYY-MM-DD），null 時預設為請求當天 */
    LocalDate endDate
) {}

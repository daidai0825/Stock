package tw.com.stockplatform.quote.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 單一股票行情 DTO（schema-lock v1.0 對齊）。
 * <p>
 * 欄位命名規則（依 20260422_schema-lock_stock-detail-apis.md §1.3）：
 * - {@code price}：當日收盤/最新成交價（原 closePrice，重命名）
 * - {@code open/high/low}：去掉 Price 字尾
 * - {@code change/changePercent}：後端計算，正值帶 "+" 前綴
 * - {@code previousClose}：前一交易日收盤價
 * - {@code updatedAt}：資料最後 upsert 時間（對應 DB createdAt，upsert 策略下即最後更新時間）
 * - {@code isStale}：週末/假日/外部失敗時 fallback 至 DB 最新一筆並標記 true
 * - {@code source}：資料來源，與 market 一致（"TWSE" 或 "OTC"）
 * <p>
 * 序列化說明：BigDecimal 欄位由 Jackson 序列化為 string（需配合 Jackson 設定）。
 */
public record QuoteDTO(
    String stockId,
    String stockName,
    String market,
    BigDecimal price,
    BigDecimal previousClose,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    Long volume,
    LocalDate quoteDate,
    LocalDateTime updatedAt,
    boolean isStale,
    String source
) {}

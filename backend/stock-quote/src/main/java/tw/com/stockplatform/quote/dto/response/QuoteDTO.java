package tw.com.stockplatform.quote.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import tw.com.stockplatform.quote.serializer.PlainDecimalSerializer;
import tw.com.stockplatform.quote.serializer.SignedDecimalSerializer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 單一股票行情 DTO（schema-lock v1.0 對齊）。
 * <p>
 * 欄位命名規則（依 20260422_schema-lock_stock-detail-apis.md §1.3）：
 * - {@code price}：當日收盤/最新成交價，序列化為 string（PlainDecimalSerializer）
 * - {@code open/high/low}：去掉 Price 字尾，序列化為 string
 * - {@code change/changePercent}：後端計算，正值帶 "+" 前綴（SignedDecimalSerializer）
 * - {@code previousClose}：前一交易日收盤價，序列化為 string
 * - {@code updatedAt}：資料最後 upsert 時間（對應 DB createdAt，upsert 策略下即最後更新時間）
 * - {@code isStale}：週末/假日/外部失敗時 fallback 至 DB 最新一筆並標記 true
 * - {@code source}：資料來源，與 market 一致（"TWSE" 或 "OTC"）
 * <p>
 * M-01 修正：BigDecimal 欄位加 @JsonSerialize，確保序列化為 string 且符合前綴規則，
 * 避免 JS 浮點誤差（price/open/high/low/previousClose 用 PlainDecimalSerializer；
 * change/changePercent 用 SignedDecimalSerializer 正值帶 "+"）。
 */
public record QuoteDTO(
    String stockId,
    String stockName,
    String market,

    @JsonSerialize(using = PlainDecimalSerializer.class)
    BigDecimal price,

    @JsonSerialize(using = PlainDecimalSerializer.class)
    BigDecimal previousClose,

    @JsonSerialize(using = SignedDecimalSerializer.class)
    BigDecimal change,

    @JsonSerialize(using = SignedDecimalSerializer.class)
    BigDecimal changePercent,

    @JsonSerialize(using = PlainDecimalSerializer.class)
    BigDecimal open,

    @JsonSerialize(using = PlainDecimalSerializer.class)
    BigDecimal high,

    @JsonSerialize(using = PlainDecimalSerializer.class)
    BigDecimal low,

    Long volume,
    LocalDate quoteDate,
    LocalDateTime updatedAt,
    boolean isStale,
    String source
) {}

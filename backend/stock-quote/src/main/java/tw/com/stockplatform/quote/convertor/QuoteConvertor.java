package tw.com.stockplatform.quote.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.common.util.BigDecimalUtils;
import tw.com.stockplatform.domain.po.StockQuotePO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEDailyQuoteDTO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEOhlcDTO;
import tw.com.stockplatform.infrastructure.client.otc.OTCDailyQuoteDTO;
import tw.com.stockplatform.quote.dto.response.HistoryItem;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 行情 PO ↔ DTO 轉換（schema-lock v1.0 對齊）。
 * <p>
 * {@code toDTO(po, previousClose, isStale)}：主要轉換方法，後端計算 change/changePercent。
 * {@code toDTOStale(po, previousClose)}：fallback 情境，isStale = true。
 * <p>
 * change/changePercent 計算規則（schema-lock §1.3）：
 * <ul>
 *   <li>change = price - previousClose，正值帶 "+" 前綴（序列化為 string）</li>
 *   <li>changePercent = change / previousClose * 100，保留 2 位小數</li>
 * </ul>
 * previousClose 若為 null（首次資料），change/changePercent 設為 "0.00"。
 */
@Mapper(componentModel = "spring")
public interface QuoteConvertor {

    /**
     * PO → QuoteDTO（正常行情，isStale = false）。
     *
     * @param po            最新行情 PO
     * @param previousClose 前一交易日收盤價（null 時 change/changePercent = 0）
     */
    default QuoteDTO toDTO(StockQuotePO po, BigDecimal previousClose) {
        return toDTOWithStale(po, previousClose, false);
    }

    /**
     * PO → QuoteDTO（fallback 行情，isStale = true）。
     *
     * @param po            最新行情 PO
     * @param previousClose 前一交易日收盤價（null 時 change/changePercent = 0）
     */
    default QuoteDTO toDTOStale(StockQuotePO po, BigDecimal previousClose) {
        return toDTOWithStale(po, previousClose, true);
    }

    private QuoteDTO toDTOWithStale(StockQuotePO po, BigDecimal previousClose, boolean isStale) {
        BigDecimal price = po.getClosePrice();
        BigDecimal prevClose = previousClose != null ? previousClose : BigDecimal.ZERO;

        BigDecimal change = BigDecimalUtils.safeSubtract(price, prevClose)
            .setScale(2, RoundingMode.HALF_UP);
        // changePercent = change / previousClose * 100；previousClose = 0 時設 0
        BigDecimal changePercent = prevClose.signum() == 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : BigDecimalUtils.safeDivide(change.multiply(new BigDecimal("100")), prevClose, 2);

        return new QuoteDTO(
            po.getStockId(),
            po.getStockName(),
            po.getMarket(),
            price != null ? price.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
            prevClose.signum() == 0 ? null : prevClose.setScale(2, RoundingMode.HALF_UP),
            change,
            changePercent,
            po.getOpenPrice() != null ? po.getOpenPrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getHighPrice() != null ? po.getHighPrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getLowPrice() != null ? po.getLowPrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getVolume(),
            po.getQuoteDate(),
            po.getCreatedAt(),   // createdAt = upsert 時間，即 updatedAt 語義
            isStale,
            po.getMarket()       // source 與 market 相同
        );
    }

    /** PO → HistoryItem（日 K 直接轉換）。 */
    default HistoryItem toHistoryItem(StockQuotePO po) {
        return new HistoryItem(
            po.getQuoteDate(),
            po.getOpenPrice() != null ? po.getOpenPrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getHighPrice() != null ? po.getHighPrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getLowPrice() != null ? po.getLowPrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getClosePrice() != null ? po.getClosePrice().setScale(2, RoundingMode.HALF_UP) : null,
            po.getVolume()
        );
    }

    default StockQuotePO fromTWSEDaily(TWSEDailyQuoteDTO dto, LocalDateTime now) {
        return StockQuotePO.builder()
            .quoteId(UUID.randomUUID().toString())
            .stockId(dto.stockId())
            .stockName(dto.stockName())
            .market("TWSE")
            .openPrice(dto.openPrice())
            .highPrice(dto.highPrice())
            .lowPrice(dto.lowPrice())
            .closePrice(dto.closePrice())
            .volume(dto.volume())
            .quoteDate(dto.tradeDate())
            .createdAt(now)
            .build();
    }

    default StockQuotePO fromTWSEOhlc(TWSEOhlcDTO dto, String stockName, LocalDateTime now) {
        return StockQuotePO.builder()
            .quoteId(UUID.randomUUID().toString())
            .stockId(dto.stockId())
            .stockName(stockName)
            .market("TWSE")
            .openPrice(dto.openPrice())
            .highPrice(dto.highPrice())
            .lowPrice(dto.lowPrice())
            .closePrice(dto.closePrice())
            .volume(dto.volume())
            .quoteDate(dto.tradeDate())
            .createdAt(now)
            .build();
    }

    default StockQuotePO fromOTCDaily(OTCDailyQuoteDTO dto, LocalDateTime now) {
        return StockQuotePO.builder()
            .quoteId(UUID.randomUUID().toString())
            .stockId(dto.stockId())
            .stockName(dto.stockName())
            .market("OTC")
            .openPrice(dto.openPrice())
            .highPrice(dto.highPrice())
            .lowPrice(dto.lowPrice())
            .closePrice(dto.closePrice())
            .volume(dto.volume())
            .quoteDate(dto.tradeDate())
            .createdAt(now)
            .build();
    }
}

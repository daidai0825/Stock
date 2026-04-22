package tw.com.stockplatform.quote.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.domain.po.StockQuotePO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEDailyQuoteDTO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEOhlcDTO;
import tw.com.stockplatform.infrastructure.client.otc.OTCDailyQuoteDTO;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 行情 PO ↔ DTO 轉換（MapStruct）。
 * <p>
 * {@code toDTO(po)} 預設 isStale = false（正常行情）。
 * {@code toDTOStale(po)} 用於 fallback 情境，標記資料非即時。
 */
@Mapper(componentModel = "spring")
public interface QuoteConvertor {

    default QuoteDTO toDTO(StockQuotePO po) {
        return toDTOWithStale(po, false);
    }

    default QuoteDTO toDTOStale(StockQuotePO po) {
        return toDTOWithStale(po, true);
    }

    private QuoteDTO toDTOWithStale(StockQuotePO po, boolean isStale) {
        return new QuoteDTO(
            po.getStockId(),
            po.getStockName(),
            po.getMarket(),
            po.getOpenPrice(),
            po.getHighPrice(),
            po.getLowPrice(),
            po.getClosePrice(),
            po.getVolume(),
            po.getQuoteDate(),
            isStale
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

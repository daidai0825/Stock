package tw.com.stockplatform.quote.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tw.com.stockplatform.domain.po.StockQuotePO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 行情資料存取層（MyBatis）。
 * 使用 #{} 避免 SQL Injection。
 * {@code findLatestByStockIds} 透過 XML Mapper 實作動態 IN 子句，避免 N+1。
 */
@Mapper
public interface StockQuoteMapper {

    @Select("""
        SELECT quote_id, stock_id, stock_name, market,
               open_price, high_price, low_price, close_price,
               volume, quote_date, created_at
        FROM stock_quote
        WHERE stock_id = #{stockId}
        ORDER BY quote_date DESC
        LIMIT 1
        """)
    Optional<StockQuotePO> findLatestByStockId(@Param("stockId") String stockId);

    /**
     * 查詢指定股票在指定日期前（不含）的最近一筆行情（即前一交易日）。
     * 用於計算 change/changePercent。
     */
    @Select("""
        SELECT quote_id, stock_id, stock_name, market,
               open_price, high_price, low_price, close_price,
               volume, quote_date, created_at
        FROM stock_quote
        WHERE stock_id = #{stockId}
          AND quote_date < #{beforeDate}
        ORDER BY quote_date DESC
        LIMIT 1
        """)
    Optional<StockQuotePO> findPreviousByStockId(
        @Param("stockId") String stockId,
        @Param("beforeDate") LocalDate beforeDate
    );

    /**
     * 批次查詢多檔最新行情（XML Mapper：StockQuoteMapper.xml#findLatestByStockIds）。
     */
    List<StockQuotePO> findLatestByStockIds(@Param("stockIds") List<String> stockIds);

    @Select("""
        SELECT quote_id, stock_id, stock_name, market,
               open_price, high_price, low_price, close_price,
               volume, quote_date, created_at
        FROM stock_quote
        WHERE stock_id = #{stockId}
          AND quote_date >= #{startDate}
          AND quote_date <= #{endDate}
        ORDER BY quote_date ASC
        """)
    List<StockQuotePO> findHistoryByStockId(
        @Param("stockId") String stockId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Insert("""
        INSERT INTO stock_quote (quote_id, stock_id, stock_name, market,
            open_price, high_price, low_price, close_price, volume, quote_date, created_at)
        VALUES (#{quoteId}, #{stockId}, #{stockName}, #{market},
            #{openPrice}, #{highPrice}, #{lowPrice}, #{closePrice}, #{volume}, #{quoteDate}, #{createdAt})
        ON CONFLICT (stock_id, quote_date) DO UPDATE
            SET open_price  = EXCLUDED.open_price,
                high_price  = EXCLUDED.high_price,
                low_price   = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                volume      = EXCLUDED.volume
        """)
    int upsert(StockQuotePO po);
}

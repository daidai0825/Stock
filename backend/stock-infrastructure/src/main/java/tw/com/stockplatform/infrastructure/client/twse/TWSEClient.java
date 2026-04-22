package tw.com.stockplatform.infrastructure.client.twse;

import java.time.LocalDate;
import java.util.List;

/**
 * 台灣證交所（TWSE）外部資料 Client 介面。
 * <p>
 * 實作層加入 {@code @Retryable}，業務層僅依賴此介面。
 */
public interface TWSEClient {

    /**
     * 查詢指定日期的上市每日收盤行情（全市場）。
     *
     * @param date 查詢日期（格式 yyyyMMdd）
     * @return 每日收盤行情清單；若交易日無資料回空集合
     */
    List<TWSEDailyQuoteDTO> fetchDailyQuotes(LocalDate date);

    /**
     * 查詢個股 OHLC 歷史行情。
     *
     * @param stockId 股票代號
     * @param date    查詢月份所在日期（TWSE 依月查詢）
     * @return 月內每日 OHLC 清單
     */
    List<TWSEOhlcDTO> fetchStockHistory(String stockId, LocalDate date);

    /**
     * 查詢三大法人買賣超（上市）。
     *
     * @param date 查詢日期
     * @return 三大法人資料清單
     */
    List<TWSEInstitutionalDTO> fetchInstitutional(LocalDate date);
}

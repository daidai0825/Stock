package tw.com.stockplatform.infrastructure.client.otc;

import java.time.LocalDate;
import java.util.List;

/**
 * 財團法人中華民國證券櫃檯買賣中心（TPEX/OTC）外部資料 Client 介面。
 */
public interface OTCClient {

    /**
     * 查詢指定日期的上櫃每日收盤行情。
     *
     * @param date 查詢日期
     * @return 上櫃每日行情清單；若無資料回空集合
     */
    List<OTCDailyQuoteDTO> fetchDailyQuotes(LocalDate date);

    /**
     * 查詢上櫃個股月歷史行情。
     *
     * @param stockId 股票代號
     * @param date    查詢月份日期
     * @return 月內每日 OHLC 清單
     */
    List<OTCOhlcDTO> fetchStockHistory(String stockId, LocalDate date);
}

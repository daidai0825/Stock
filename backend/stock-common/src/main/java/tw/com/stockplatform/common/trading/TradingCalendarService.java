package tw.com.stockplatform.common.trading;

import java.time.LocalDate;

/**
 * 交易日曆服務介面。
 * <p>
 * Wave 2 提供簡易實作；Wave 3（TD-7）升級為從 DB 讀取完整國定假日表。
 */
public interface TradingCalendarService {

    /**
     * 判斷 DB 最新資料日期是否屬於「有效的最近交易日」。
     * <p>
     * 若為 true，呼叫方應直接回 DB 資料（可選擇性標記 isStale）；
     * 若為 false，應嘗試呼叫外部資料源，外部亦無資料時再 fallback 回 DB + isStale=true。
     *
     * @param dataDate 資料日期（DB 最新一筆）
     * @param today    系統今日（GMT+8）
     * @return true 表示資料在有效範圍內
     */
    boolean isValidRecentTradingDay(LocalDate dataDate, LocalDate today);

    /**
     * 判斷 today 是否本身就是非交易日（週末或假日）。
     * <p>
     * Wave 2：僅週末判斷；Wave 3 補全國定假日。
     *
     * @param date 日期
     * @return true 表示非交易日
     */
    boolean isNonTradingDay(LocalDate date);
}

package tw.com.stockplatform.common.trading;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 交易日判斷工具。
 * <p>
 * 短期策略（Wave 2）：
 * <ul>
 *   <li>週六/週日 → 非交易日</li>
 *   <li>距今 ≤ 3 個工作日內的 DB 最新一筆 → 視為有效（isStale = false）</li>
 *   <li>完整國定假日表留待 Wave 3（TD-7）補充</li>
 * </ul>
 *
 * <p>Wave 3 TODO (TD-7): 引入 HolidayCalendarRepository，從 DB 讀取完整國定假日清單。
 */
public final class TradingDayResolver {

    /** 允許的最大「過期」工作日數。DB 最新一筆若在此範圍內，仍視為有效。 */
    static final int MAX_STALE_WORKING_DAYS = 3;

    private TradingDayResolver() {
    }

    /**
     * 判斷給定日期是否為週末（簡易版，不含國定假日）。
     *
     * @param date 待判斷日期
     * @return true 表示週末（非交易日）
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * 判斷 DB 回傳的最新資料日期是否在有效的「最近交易日」範圍內。
     * <p>
     * 規則：從 today 往回算，跳過週末，計算工作日數，若 quoteDate 距今 ≤ MAX_STALE_WORKING_DAYS
     * 個工作日，則視為「仍有效的最近交易日資料」。
     *
     * @param quoteDate DB 最新資料日期
     * @param today     系統今日日期（GMT+8）
     * @return true 表示在有效範圍內（不算過期）
     */
    public static boolean isWithinRecentTradingDays(LocalDate quoteDate, LocalDate today) {
        if (quoteDate == null || quoteDate.isAfter(today)) {
            return false;
        }
        if (quoteDate.isEqual(today)) {
            return true;
        }
        // 往回數工作日
        int workingDaysPassed = 0;
        LocalDate cursor = today;
        while (cursor.isAfter(quoteDate) && workingDaysPassed <= MAX_STALE_WORKING_DAYS) {
            cursor = cursor.minusDays(1);
            if (!isWeekend(cursor)) {
                workingDaysPassed++;
            }
        }
        return cursor.isEqual(quoteDate) && workingDaysPassed <= MAX_STALE_WORKING_DAYS;
    }

    /**
     * 判斷 DB 最新資料是否為「今日」（精確比對）。
     *
     * @param quoteDate DB 最新資料日期
     * @param today     系統今日日期（GMT+8）
     * @return true 表示資料就是今日
     */
    public static boolean isToday(LocalDate quoteDate, LocalDate today) {
        return today.equals(quoteDate);
    }
}

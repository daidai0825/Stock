package tw.com.stockplatform.common.trading;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 交易日曆服務簡易實作（Wave 2）。
 * <p>
 * 規則：
 * <ul>
 *   <li>週六/週日 → 非交易日</li>
 *   <li>DB 最新資料 ≤ 3 個工作日前 → 視為有效</li>
 * </ul>
 * Wave 3 (TD-7) 應替換為讀取完整國定假日 DB 表的實作。
 */
@Slf4j
@Service
public class TradingCalendarServiceImpl implements TradingCalendarService {

    @Override
    public boolean isValidRecentTradingDay(LocalDate dataDate, LocalDate today) {
        boolean valid = TradingDayResolver.isWithinRecentTradingDays(dataDate, today);
        if (!valid) {
            log.debug("TradingCalendar: dataDate={} is NOT within recent trading days (today={})", dataDate, today);
        }
        return valid;
    }

    @Override
    public boolean isNonTradingDay(LocalDate date) {
        return TradingDayResolver.isWeekend(date);
    }
}

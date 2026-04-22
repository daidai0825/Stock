package tw.com.stockplatform.common.trading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TradingDayResolver 單元測試。
 */
class TradingDayResolverTest {

    // --- isWeekend ---

    @Test
    @DisplayName("週六 → 是週末")
    void isWeekend_saturday() {
        LocalDate saturday = LocalDate.of(2026, 4, 18); // 週六
        assertThat(TradingDayResolver.isWeekend(saturday)).isTrue();
    }

    @Test
    @DisplayName("週日 → 是週末")
    void isWeekend_sunday() {
        LocalDate sunday = LocalDate.of(2026, 4, 19); // 週日
        assertThat(TradingDayResolver.isWeekend(sunday)).isTrue();
    }

    @Test
    @DisplayName("週一 → 非週末")
    void isWeekend_monday() {
        LocalDate monday = LocalDate.of(2026, 4, 20); // 週一
        assertThat(TradingDayResolver.isWeekend(monday)).isFalse();
    }

    @Test
    @DisplayName("週五 → 非週末")
    void isWeekend_friday() {
        LocalDate friday = LocalDate.of(2026, 4, 17); // 週五
        assertThat(TradingDayResolver.isWeekend(friday)).isFalse();
    }

    // --- isWithinRecentTradingDays ---

    @Test
    @DisplayName("今日資料 → 有效（不算過期）")
    void isWithinRecentTradingDays_today_valid() {
        LocalDate today = LocalDate.of(2026, 4, 22); // 週三
        assertThat(TradingDayResolver.isWithinRecentTradingDays(today, today)).isTrue();
    }

    @Test
    @DisplayName("週一行情，週二查詢（1 個工作日）→ 有效")
    void isWithinRecentTradingDays_oneDayAgo_valid() {
        LocalDate monday = LocalDate.of(2026, 4, 20);
        LocalDate tuesday = LocalDate.of(2026, 4, 21);
        assertThat(TradingDayResolver.isWithinRecentTradingDays(monday, tuesday)).isTrue();
    }

    @Test
    @DisplayName("週五行情，週一查詢（跨週末，1 個工作日）→ 有效")
    void isWithinRecentTradingDays_friday_queriedOnMonday_valid() {
        LocalDate friday = LocalDate.of(2026, 4, 17);
        LocalDate monday = LocalDate.of(2026, 4, 20);
        // 週六週日不算工作日；週一往回數：週五 = 1 個工作日
        assertThat(TradingDayResolver.isWithinRecentTradingDays(friday, monday)).isTrue();
    }

    @Test
    @DisplayName("週五行情，週三查詢（跨週末，3 個工作日）→ 有效（≤3）")
    void isWithinRecentTradingDays_friday_queriedOnWednesday_valid() {
        LocalDate friday = LocalDate.of(2026, 4, 17);
        LocalDate wednesday = LocalDate.of(2026, 4, 22);
        // 週三往回：週二(1)、週一(2)、週五(3) → 3 個工作日，剛好邊界值
        assertThat(TradingDayResolver.isWithinRecentTradingDays(friday, wednesday)).isTrue();
    }

    @Test
    @DisplayName("超過 3 個工作日 → 無效（過期）")
    void isWithinRecentTradingDays_tooOld_invalid() {
        LocalDate oldDate = LocalDate.of(2026, 4, 15); // 週三
        LocalDate today = LocalDate.of(2026, 4, 22);   // 週三，相差 5 個工作日
        assertThat(TradingDayResolver.isWithinRecentTradingDays(oldDate, today)).isFalse();
    }

    @Test
    @DisplayName("quoteDate 為 null → 無效")
    void isWithinRecentTradingDays_nullDate_invalid() {
        assertThat(TradingDayResolver.isWithinRecentTradingDays(null, LocalDate.now())).isFalse();
    }

    @Test
    @DisplayName("quoteDate 在 today 之後 → 無效（未來日期）")
    void isWithinRecentTradingDays_futureDate_invalid() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThat(TradingDayResolver.isWithinRecentTradingDays(tomorrow, LocalDate.now())).isFalse();
    }

    // --- isToday ---

    @Test
    @DisplayName("quoteDate == today → true")
    void isToday_sameDate() {
        LocalDate date = LocalDate.of(2026, 4, 22);
        assertThat(TradingDayResolver.isToday(date, date)).isTrue();
    }

    @Test
    @DisplayName("quoteDate != today → false")
    void isToday_differentDate() {
        LocalDate date = LocalDate.of(2026, 4, 21);
        LocalDate today = LocalDate.of(2026, 4, 22);
        assertThat(TradingDayResolver.isToday(date, today)).isFalse();
    }
}

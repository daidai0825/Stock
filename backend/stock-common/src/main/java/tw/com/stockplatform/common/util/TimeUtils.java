package tw.com.stockplatform.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 固定時區（GMT+8）時間工具。
 */
public final class TimeUtils {

    public static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    public static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private TimeUtils() {
    }

    /**
     * 當下時間（GMT+8 LocalDateTime）。
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(TAIPEI);
    }

    /**
     * 今日日期（GMT+8 LocalDate）。
     */
    public static LocalDate today() {
        return LocalDate.now(TAIPEI);
    }

    /**
     * 當下時間 ISO 8601 字串（含 +08:00 偏移）。
     */
    public static String nowIso8601() {
        return ZonedDateTime.now(TAIPEI).format(ISO_OFFSET);
    }

    /**
     * LocalDateTime 轉 ISO 8601 字串（套用 GMT+8 偏移）。
     */
    public static String toIso8601(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(TAIPEI).format(ISO_OFFSET);
    }
}

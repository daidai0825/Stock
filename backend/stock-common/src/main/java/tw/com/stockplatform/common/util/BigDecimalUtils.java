package tw.com.stockplatform.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal 安全運算工具。
 * <p>
 * 全平台禁止 double / float 處理金額；非 BigDecimal 型別請先轉換再運算。
 */
public final class BigDecimalUtils {

    /** 預設小數位 */
    public static final int DEFAULT_SCALE = 4;

    /** 預設四捨五入策略 */
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private BigDecimalUtils() {
    }

    /**
     * null safe 加法，null 視為 0。
     */
    public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return nullToZero(a).add(nullToZero(b));
    }

    /**
     * null safe 減法。
     */
    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        return nullToZero(a).subtract(nullToZero(b));
    }

    /**
     * null safe 乘法。
     */
    public static BigDecimal safeMultiply(BigDecimal a, BigDecimal b) {
        return nullToZero(a).multiply(nullToZero(b));
    }

    /**
     * null safe 除法（除數為 0 或 null 回傳 ZERO，避免 ArithmeticException）。
     */
    public static BigDecimal safeDivide(BigDecimal a, BigDecimal b, int scale) {
        if (a == null || b == null || b.signum() == 0) {
            return BigDecimal.ZERO.setScale(scale, DEFAULT_ROUNDING);
        }
        return a.divide(b, scale, DEFAULT_ROUNDING);
    }

    public static BigDecimal safeDivide(BigDecimal a, BigDecimal b) {
        return safeDivide(a, b, DEFAULT_SCALE);
    }

    /**
     * 套用預設小數位四捨五入。
     */
    public static BigDecimal scale(BigDecimal value) {
        return scale(value, DEFAULT_SCALE);
    }

    public static BigDecimal scale(BigDecimal value, int scale) {
        return nullToZero(value).setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * double 轉 BigDecimal（用於外部資料源轉入）。
     * <p>
     * 內部運算禁止使用 double，本方法僅用於資料邊界轉換。
     */
    public static BigDecimal fromDouble(double value) {
        return BigDecimal.valueOf(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

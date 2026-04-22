package tw.com.stockplatform.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalUtilsTest {

    @Test
    @DisplayName("safeAdd：null 視為 0，回傳兩數和")
    void safeAdd_NullSafe() {
        assertThat(BigDecimalUtils.safeAdd(null, new BigDecimal("3.14")))
            .isEqualByComparingTo("3.14");
        assertThat(BigDecimalUtils.safeAdd(new BigDecimal("1.5"), new BigDecimal("2.5")))
            .isEqualByComparingTo("4.0");
    }

    @Test
    @DisplayName("safeMultiply：BigDecimal 精度不失真")
    void safeMultiply_KeepsPrecision() {
        BigDecimal price = new BigDecimal("10.0001");
        BigDecimal qty = new BigDecimal("3");
        assertThat(BigDecimalUtils.safeMultiply(price, qty))
            .isEqualByComparingTo("30.0003");
    }

    @Test
    @DisplayName("safeDivide：除數為 0 回傳 ZERO 而非拋例外")
    void safeDivide_ZeroDivisor() {
        assertThat(BigDecimalUtils.safeDivide(new BigDecimal("10"), BigDecimal.ZERO))
            .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("scale：套用預設 4 位小數 + HALF_UP")
    void scale_DefaultFourDigits() {
        assertThat(BigDecimalUtils.scale(new BigDecimal("1.23456789")))
            .isEqualByComparingTo("1.2346");
    }

    @Test
    @DisplayName("nullToZero：null 轉 0")
    void nullToZero() {
        assertThat(BigDecimalUtils.nullToZero(null)).isEqualByComparingTo("0");
        assertThat(BigDecimalUtils.nullToZero(new BigDecimal("5"))).isEqualByComparingTo("5");
    }
}

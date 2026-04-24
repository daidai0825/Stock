package tw.com.stockplatform.quote.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QuoteDTO 序列化測試（M-01 驗收）。
 * <p>
 * 驗證 schema-lock §1.3 序列化規則：
 * - price / open / high / low / previousClose → 純字串（無正號）
 * - change / changePercent → 正值帶 "+" 前綴、負值保留負號
 */
class QuoteDTOSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("正值 change/changePercent 序列化帶 '+' 前綴，price/open/high/low 為純字串")
    void serialize_positiveChange_hasSignPrefix() throws Exception {
        QuoteDTO dto = new QuoteDTO(
            "2330", "台積電", "TWSE",
            new BigDecimal("548.00"),   // price
            new BigDecimal("529.00"),   // previousClose
            new BigDecimal("19.00"),    // change（正值）
            new BigDecimal("1.84"),     // changePercent（正值）
            new BigDecimal("530.00"),   // open
            new BigDecimal("550.00"),   // high
            new BigDecimal("528.00"),   // low
            50_000_000L,
            LocalDate.of(2026, 4, 23),
            LocalDateTime.of(2026, 4, 23, 15, 0),
            false,
            "TWSE"
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"price\":\"548.00\"");
        assertThat(json).contains("\"change\":\"+19.00\"");
        assertThat(json).contains("\"changePercent\":\"+1.84\"");
        assertThat(json).contains("\"previousClose\":\"529.00\"");
        assertThat(json).contains("\"open\":\"530.00\"");
        assertThat(json).contains("\"high\":\"550.00\"");
        assertThat(json).contains("\"low\":\"528.00\"");
    }

    @Test
    @DisplayName("負值 change/changePercent 序列化保留負號，不加 '+'")
    void serialize_negativeChange_hasMinusSign() throws Exception {
        QuoteDTO dto = new QuoteDTO(
            "2330", "台積電", "TWSE",
            new BigDecimal("524.00"),   // price
            new BigDecimal("529.00"),   // previousClose
            new BigDecimal("-5.00"),    // change（負值）
            new BigDecimal("-0.95"),    // changePercent（負值）
            new BigDecimal("528.00"),
            new BigDecimal("530.00"),
            new BigDecimal("522.00"),
            30_000_000L,
            LocalDate.of(2026, 4, 23),
            LocalDateTime.of(2026, 4, 23, 15, 0),
            false,
            "TWSE"
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"price\":\"524.00\"");
        assertThat(json).contains("\"change\":\"-5.00\"");
        assertThat(json).contains("\"changePercent\":\"-0.95\"");
        // 確認不含 "+-"（錯誤格式）
        assertThat(json).doesNotContain("\"+-\"");
    }

    @Test
    @DisplayName("零值 change/changePercent 序列化帶 '+' 前綴")
    void serialize_zeroChange_hasPositivePrefix() throws Exception {
        QuoteDTO dto = new QuoteDTO(
            "2330", "台積電", "TWSE",
            new BigDecimal("529.00"),
            new BigDecimal("529.00"),
            new BigDecimal("0.00"),    // change = 0
            new BigDecimal("0.00"),    // changePercent = 0
            new BigDecimal("529.00"),
            new BigDecimal("531.00"),
            new BigDecimal("527.00"),
            20_000_000L,
            LocalDate.of(2026, 4, 23),
            LocalDateTime.of(2026, 4, 23, 15, 0),
            false,
            "TWSE"
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"change\":\"+0.00\"");
        assertThat(json).contains("\"changePercent\":\"+0.00\"");
        // price 不帶 + 前綴
        assertThat(json).contains("\"price\":\"529.00\"");
        assertThat(json).doesNotContain("\"price\":\"+");
    }
}

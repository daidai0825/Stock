package tw.com.stockplatform.quote.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trading.TradingCalendarService;
import tw.com.stockplatform.domain.po.StockQuotePO;
import tw.com.stockplatform.infrastructure.client.otc.OTCClient;
import tw.com.stockplatform.infrastructure.client.twse.TWSEClient;
import tw.com.stockplatform.quote.convertor.QuoteConvertor;
import tw.com.stockplatform.quote.dto.request.QuoteHistoryRequest;
import tw.com.stockplatform.quote.dto.response.HistoryItem;
import tw.com.stockplatform.quote.dto.response.QuoteHistoryResponse;
import tw.com.stockplatform.quote.enums.KLinePeriod;
import tw.com.stockplatform.quote.repository.StockQuoteMapper;
import tw.com.stockplatform.quote.service.impl.QuoteServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * QuoteServiceImpl 週/月聚合邏輯單元測試（Wave 2 Round 2）。
 * <p>
 * 重點驗證：
 * <ul>
 *   <li>daily：直接回傳日線資料</li>
 *   <li>weekly：正確依自然週聚合（open=週首日open, high=週最高, low=週最低, close=週末日close, volume=加總）</li>
 *   <li>monthly：正確依自然月聚合</li>
 *   <li>startDate > endDate → 1002</li>
 *   <li>範圍超出最長限制 → 1003</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
// 參數驗證類測試（StartDateAfterEndDate / ExceedsMaxRange）會在進入 cache lookup 前拋例外，
// 共用的 setUp() Redis stub 在這些案例中變成 unnecessary stubbing。
// 採 LENIENT 允許共用 stub 並列存在；個別測試仍可透過 verify 驗證互動次數。
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteHistoryAggregationTest {

    @Mock
    private StockQuoteMapper quoteMapper;

    @Mock
    private TWSEClient twseClient;

    @Mock
    private OTCClient otcClient;

    @Mock
    private QuoteConvertor quoteConvertor;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private TradingCalendarService tradingCalendarService;

    @InjectMocks
    private QuoteServiceImpl quoteService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // cache miss
    }

    @Test
    @DisplayName("getHistory daily：回傳日線 HistoryItem，每筆一對一對應")
    void getHistory_Daily_ReturnsDailyItems() {
        // 2026-04-14（週一）、2026-04-15（週二）兩筆日線
        List<StockQuotePO> dbRows = List.of(
            buildPO("2026-04-14", "1020", "1035", "1015", "1031", 25_000_000L),
            buildPO("2026-04-15", "1033", "1058", "1029", "1050", 28_000_000L)
        );
        when(quoteMapper.findHistoryByStockId(anyString(), any(), any())).thenReturn(dbRows);
        when(quoteConvertor.toHistoryItem(any())).thenAnswer(inv -> {
            StockQuotePO po = inv.getArgument(0);
            return new HistoryItem(po.getQuoteDate(), po.getOpenPrice(), po.getHighPrice(),
                po.getLowPrice(), po.getClosePrice(), po.getVolume());
        });

        QuoteHistoryRequest req = new QuoteHistoryRequest("2330", KLinePeriod.daily,
            LocalDate.of(2026, 4, 14), LocalDate.of(2026, 4, 15));
        QuoteHistoryResponse resp = quoteService.getHistory(req);

        assertThat(resp.period()).isEqualTo(KLinePeriod.daily);
        assertThat(resp.items()).hasSize(2);
        assertThat(resp.items().get(0).date()).isEqualTo(LocalDate.of(2026, 4, 14));
        assertThat(resp.items().get(1).date()).isEqualTo(LocalDate.of(2026, 4, 15));
    }

    @Test
    @DisplayName("getHistory weekly：同一週三筆日線 → 聚合為 1 筆週 K")
    void getHistory_Weekly_AggregatesSameWeek() {
        // 2026-04-14（週一）、2026-04-15（週二）、2026-04-16（週三）—— 同一週
        List<StockQuotePO> dbRows = List.of(
            buildPO("2026-04-14", "1020", "1040", "1015", "1030", 20_000_000L),
            buildPO("2026-04-15", "1032", "1060", "1028", "1055", 30_000_000L),
            buildPO("2026-04-16", "1050", "1058", "1045", "1048", 25_000_000L)
        );
        when(quoteMapper.findHistoryByStockId(anyString(), any(), any())).thenReturn(dbRows);

        QuoteHistoryRequest req = new QuoteHistoryRequest("2330", KLinePeriod.weekly,
            LocalDate.of(2026, 4, 14), LocalDate.of(2026, 4, 16));
        QuoteHistoryResponse resp = quoteService.getHistory(req);

        assertThat(resp.period()).isEqualTo(KLinePeriod.weekly);
        assertThat(resp.items()).hasSize(1);  // 3 天都在同一週

        HistoryItem item = resp.items().get(0);
        assertThat(item.date()).isEqualTo(LocalDate.of(2026, 4, 14));           // 週首日
        assertThat(item.open()).isEqualByComparingTo("1020.00");                // 週首日 open
        assertThat(item.high()).isEqualByComparingTo("1060.00");                // 週最高 high
        assertThat(item.low()).isEqualByComparingTo("1015.00");                 // 週最低 low
        assertThat(item.close()).isEqualByComparingTo("1048.00");               // 週末日 close
        assertThat(item.volume()).isEqualTo(20_000_000L + 30_000_000L + 25_000_000L); // 週加總
    }

    @Test
    @DisplayName("getHistory weekly：跨兩週的資料 → 聚合為 2 筆週 K")
    void getHistory_Weekly_TwoWeeks() {
        // 第一週：2026-04-14（週一）、2026-04-15（週二）
        // 第二週：2026-04-20（週一）、2026-04-21（週二）
        List<StockQuotePO> dbRows = List.of(
            buildPO("2026-04-14", "1020", "1035", "1015", "1031", 25_000_000L),
            buildPO("2026-04-15", "1033", "1058", "1029", "1050", 28_000_000L),
            buildPO("2026-04-20", "1045", "1070", "1040", "1065", 30_000_000L),
            buildPO("2026-04-21", "1060", "1080", "1055", "1075", 35_000_000L)
        );
        when(quoteMapper.findHistoryByStockId(anyString(), any(), any())).thenReturn(dbRows);

        QuoteHistoryRequest req = new QuoteHistoryRequest("2330", KLinePeriod.weekly,
            LocalDate.of(2026, 4, 14), LocalDate.of(2026, 4, 21));
        QuoteHistoryResponse resp = quoteService.getHistory(req);

        assertThat(resp.items()).hasSize(2);

        // 第一週
        HistoryItem week1 = resp.items().get(0);
        assertThat(week1.date()).isEqualTo(LocalDate.of(2026, 4, 14));
        assertThat(week1.open()).isEqualByComparingTo("1020.00");
        assertThat(week1.high()).isEqualByComparingTo("1058.00");
        assertThat(week1.low()).isEqualByComparingTo("1015.00");
        assertThat(week1.close()).isEqualByComparingTo("1050.00");
        assertThat(week1.volume()).isEqualTo(53_000_000L);

        // 第二週
        HistoryItem week2 = resp.items().get(1);
        assertThat(week2.date()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(week2.open()).isEqualByComparingTo("1045.00");
        assertThat(week2.high()).isEqualByComparingTo("1080.00");
        assertThat(week2.low()).isEqualByComparingTo("1040.00");
        assertThat(week2.close()).isEqualByComparingTo("1075.00");
        assertThat(week2.volume()).isEqualTo(65_000_000L);
    }

    @Test
    @DisplayName("getHistory monthly：跨兩月的資料 → 聚合為 2 筆月 K")
    void getHistory_Monthly_TwoMonths() {
        // 3 月：2026-03-01、2026-03-31
        // 4 月：2026-04-01、2026-04-22
        List<StockQuotePO> dbRows = List.of(
            buildPO("2026-03-01", "950", "970", "940", "960", 20_000_000L),
            buildPO("2026-03-31", "960", "990", "955", "985", 25_000_000L),
            buildPO("2026-04-01", "990", "1010", "985", "1005", 22_000_000L),
            buildPO("2026-04-22", "1030", "1060", "1025", "1050", 28_000_000L)
        );
        when(quoteMapper.findHistoryByStockId(anyString(), any(), any())).thenReturn(dbRows);

        QuoteHistoryRequest req = new QuoteHistoryRequest("2330", KLinePeriod.monthly,
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 22));
        QuoteHistoryResponse resp = quoteService.getHistory(req);

        assertThat(resp.items()).hasSize(2);

        // 3 月
        HistoryItem mar = resp.items().get(0);
        assertThat(mar.date()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(mar.open()).isEqualByComparingTo("950.00");      // 月首日 open
        assertThat(mar.high()).isEqualByComparingTo("990.00");      // 月最高 high
        assertThat(mar.low()).isEqualByComparingTo("940.00");       // 月最低 low
        assertThat(mar.close()).isEqualByComparingTo("985.00");     // 月末日 close
        assertThat(mar.volume()).isEqualTo(45_000_000L);

        // 4 月
        HistoryItem apr = resp.items().get(1);
        assertThat(apr.date()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(apr.high()).isEqualByComparingTo("1060.00");
        assertThat(apr.low()).isEqualByComparingTo("985.00");
        assertThat(apr.close()).isEqualByComparingTo("1050.00");
    }

    @Test
    @DisplayName("getHistory：startDate > endDate → 拋 PARAM_FORMAT_INVALID（1002）")
    void getHistory_StartDateAfterEndDate_ThrowsParamFormatInvalid() {
        QuoteHistoryRequest req = new QuoteHistoryRequest("2330", KLinePeriod.daily,
            LocalDate.of(2026, 4, 22), LocalDate.of(2026, 4, 1)); // 反序

        assertThatThrownBy(() -> quoteService.getHistory(req))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(1002));
    }

    @Test
    @DisplayName("getHistory daily：超出 5 年（1825 天）→ 拋 PARAM_OUT_OF_RANGE（1003）")
    void getHistory_Daily_ExceedsMaxRange_ThrowsParamOutOfRange() {
        QuoteHistoryRequest req = new QuoteHistoryRequest("2330", KLinePeriod.daily,
            LocalDate.of(2020, 1, 1), LocalDate.of(2026, 4, 22)); // > 5 年

        assertThatThrownBy(() -> quoteService.getHistory(req))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(1003));
    }

    // --- 輔助方法 ---

    private StockQuotePO buildPO(String date, String open, String high, String low, String close, long volume) {
        return StockQuotePO.builder()
            .quoteId("uuid-" + date)
            .stockId("2330")
            .stockName("台積電")
            .market("TWSE")
            .openPrice(new BigDecimal(open))
            .highPrice(new BigDecimal(high))
            .lowPrice(new BigDecimal(low))
            .closePrice(new BigDecimal(close))
            .volume(volume)
            .quoteDate(LocalDate.parse(date))
            .build();
    }
}

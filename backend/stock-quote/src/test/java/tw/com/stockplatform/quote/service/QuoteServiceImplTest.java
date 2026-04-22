package tw.com.stockplatform.quote.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trading.TradingCalendarService;
import tw.com.stockplatform.domain.po.StockQuotePO;
import tw.com.stockplatform.infrastructure.client.otc.OTCClient;
import tw.com.stockplatform.infrastructure.client.twse.TWSEClient;
import tw.com.stockplatform.infrastructure.client.twse.TWSEDailyQuoteDTO;
import tw.com.stockplatform.quote.convertor.QuoteConvertor;
import tw.com.stockplatform.quote.dto.request.QuoteGetRequest;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;
import tw.com.stockplatform.quote.enums.KLinePeriod;
import tw.com.stockplatform.quote.repository.StockQuoteMapper;
import tw.com.stockplatform.quote.service.impl.QuoteServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * QuoteServiceImpl 單元測試（Wave 2 Round 2 更新）。
 * <p>
 * Round 2 變更：QuoteDTO 結構改為 schema-lock v1.0（price/change/changePercent/previousClose/updatedAt/source）。
 */
@ExtendWith(MockitoExtension.class)
class QuoteServiceImplTest {

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
    }

    // --- getQuote 測試 ---

    @Test
    @DisplayName("getQuote：Redis cache hit 時直接回傳 DTO，不呼叫 DB")
    void getQuote_CacheHit() {
        QuoteDTO cachedDto = buildQuoteDTO("2330", false);
        when(valueOperations.get(anyString())).thenReturn(cachedDto);

        QuoteDTO result = quoteService.getQuote(new QuoteGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        verify(quoteMapper, never()).findLatestByStockId(anyString());
    }

    @Test
    @DisplayName("getQuote：DB 有今日資料時回傳 DB 資料（isStale=false）")
    void getQuote_DbHit_today() {
        LocalDate today = LocalDate.now();
        StockQuotePO po = buildQuotePO("2330", today);
        QuoteDTO dto = buildQuoteDTO("2330", false);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(quoteMapper.findLatestByStockId("2330")).thenReturn(Optional.of(po));
        when(tradingCalendarService.isValidRecentTradingDay(today, today)).thenReturn(true);
        when(quoteMapper.findPreviousByStockId(eq("2330"), eq(today))).thenReturn(Optional.empty());
        when(quoteConvertor.toDTO(eq(po), any())).thenReturn(dto);

        QuoteDTO result = quoteService.getQuote(new QuoteGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        assertThat(result.isStale()).isFalse();
        verify(valueOperations).set(anyString(), eq(dto), anyLong(), eq(SECONDS));
    }

    @Test
    @DisplayName("getQuote（B-BE-W2-01）：週末查詢，DB 有前一交易日資料 → 回 DB + isStale=true")
    void getQuote_Weekend_DbHasPreviousTradingDay_ReturnStale() {
        LocalDate friday = LocalDate.of(2026, 4, 17);
        LocalDate saturday = LocalDate.of(2026, 4, 18);
        StockQuotePO po = buildQuotePO("2330", friday);
        QuoteDTO staleDto = buildQuoteDTO("2330", true);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(quoteMapper.findLatestByStockId("2330")).thenReturn(Optional.of(po));
        when(tradingCalendarService.isValidRecentTradingDay(eq(friday), any())).thenReturn(true);
        when(quoteMapper.findPreviousByStockId(eq("2330"), eq(friday))).thenReturn(Optional.empty());
        when(quoteConvertor.toDTOStale(eq(po), any())).thenReturn(staleDto);

        QuoteDTO result = quoteService.getQuote(new QuoteGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        assertThat(result.isStale()).isTrue();
    }

    @Test
    @DisplayName("getQuote（B-BE-W2-01）：外部回空（週末），DB 有最新一筆 → fallback DB + isStale=true")
    void getQuote_ExternalEmpty_FallbackToDbStale() {
        LocalDate friday = LocalDate.of(2026, 4, 17);
        StockQuotePO po = buildQuotePO("2330", friday);
        QuoteDTO staleDto = buildQuoteDTO("2330", true);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(quoteMapper.findLatestByStockId("2330")).thenReturn(Optional.of(po));
        when(tradingCalendarService.isValidRecentTradingDay(any(), any())).thenReturn(false);
        when(twseClient.fetchDailyQuotes(any())).thenReturn(List.of());
        when(otcClient.fetchDailyQuotes(any())).thenReturn(List.of());
        when(quoteMapper.findPreviousByStockId(eq("2330"), eq(friday))).thenReturn(Optional.empty());
        when(quoteConvertor.toDTOStale(eq(po), any())).thenReturn(staleDto);

        QuoteDTO result = quoteService.getQuote(new QuoteGetRequest("2330"));

        assertThat(result.isStale()).isTrue();
        verify(quoteConvertor).toDTOStale(eq(po), any());
    }

    @Test
    @DisplayName("getQuote（B-BE-W2-01）：DB 無資料 + 外部無資料 → 拋 STOCK_NOT_FOUND")
    void getQuote_NoDbNoExternal_ThrowsStockNotFound() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(quoteMapper.findLatestByStockId("9999")).thenReturn(Optional.empty());
        when(tradingCalendarService.isValidRecentTradingDay(any(), any())).thenReturn(false);
        when(twseClient.fetchDailyQuotes(any())).thenReturn(List.of());
        when(otcClient.fetchDailyQuotes(any())).thenReturn(List.of());

        assertThatThrownBy(() -> quoteService.getQuote(new QuoteGetRequest("9999")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getQuote：DB 無資料時呼叫 TWSE Client，成功則 isStale=false")
    void getQuote_FetchFromTWSE_Success() {
        TWSEDailyQuoteDTO twseDto = buildTWSEDto("2330");
        StockQuotePO po = buildQuotePO("2330", LocalDate.now());
        QuoteDTO dto = buildQuoteDTO("2330", false);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(quoteMapper.findLatestByStockId("2330")).thenReturn(Optional.empty());
        when(tradingCalendarService.isValidRecentTradingDay(any(), any())).thenReturn(false);
        when(twseClient.fetchDailyQuotes(any())).thenReturn(List.of(twseDto));
        when(quoteConvertor.fromTWSEDaily(any(), any())).thenReturn(po);
        when(quoteMapper.upsert(any())).thenReturn(1);
        when(quoteMapper.findPreviousByStockId(eq("2330"), any())).thenReturn(Optional.empty());
        when(quoteConvertor.toDTO(eq(po), any())).thenReturn(dto);

        QuoteDTO result = quoteService.getQuote(new QuoteGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        assertThat(result.isStale()).isFalse();
        verify(twseClient).fetchDailyQuotes(any(LocalDate.class));
    }

    @Test
    @DisplayName("getQuote：有 previousClose 時 change 應為 price - previousClose（Convertor 驗證）")
    void getQuote_WithPreviousClose_ChangeCalculated() {
        LocalDate today = LocalDate.now();
        StockQuotePO latestPo = buildQuotePO("2330", today);
        StockQuotePO prevPo = buildQuotePO("2330", today.minusDays(1));
        prevPo.setClosePrice(new BigDecimal("1031.00"));

        QuoteDTO dto = buildQuoteDTO("2330", false);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(quoteMapper.findLatestByStockId("2330")).thenReturn(Optional.of(latestPo));
        when(tradingCalendarService.isValidRecentTradingDay(today, today)).thenReturn(true);
        when(quoteMapper.findPreviousByStockId(eq("2330"), eq(today))).thenReturn(Optional.of(prevPo));
        when(quoteConvertor.toDTO(eq(latestPo), eq(new BigDecimal("1031.00")))).thenReturn(dto);

        quoteService.getQuote(new QuoteGetRequest("2330"));

        // 驗證 Convertor 收到正確的 previousClose
        verify(quoteConvertor).toDTO(eq(latestPo), eq(new BigDecimal("1031.00")));
    }

    // --- 輔助方法 ---

    private QuoteDTO buildQuoteDTO(String stockId, boolean isStale) {
        return new QuoteDTO(
            stockId, "測試", "TWSE",
            new BigDecimal("567.00"),         // price
            new BigDecimal("558.00"),         // previousClose
            new BigDecimal("+9.00"),          // change
            new BigDecimal("+1.61"),          // changePercent
            new BigDecimal("560.00"),         // open
            new BigDecimal("570.00"),         // high
            new BigDecimal("558.00"),         // low
            10_000_000L,                      // volume
            LocalDate.now(),                  // quoteDate
            LocalDateTime.now(),              // updatedAt
            isStale,
            "TWSE"                            // source
        );
    }

    private StockQuotePO buildQuotePO(String stockId, LocalDate date) {
        return StockQuotePO.builder()
            .quoteId("uuid-1")
            .stockId(stockId)
            .stockName("測試")
            .market("TWSE")
            .openPrice(new BigDecimal("560"))
            .highPrice(new BigDecimal("570"))
            .lowPrice(new BigDecimal("558"))
            .closePrice(new BigDecimal("567"))
            .volume(10_000_000L)
            .quoteDate(date)
            .build();
    }

    private TWSEDailyQuoteDTO buildTWSEDto(String stockId) {
        return new TWSEDailyQuoteDTO(
            stockId, "測試", LocalDate.now(),
            new BigDecimal("560"), new BigDecimal("570"), new BigDecimal("558"),
            new BigDecimal("567"), 10_000_000L
        );
    }
}

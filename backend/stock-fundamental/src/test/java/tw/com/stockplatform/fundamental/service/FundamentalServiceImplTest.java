package tw.com.stockplatform.fundamental.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tw.com.stockplatform.domain.po.StockFundamentalPO;
import tw.com.stockplatform.fundamental.convertor.FundamentalConvertor;
import tw.com.stockplatform.fundamental.dto.request.FundamentalGetRequest;
import tw.com.stockplatform.fundamental.dto.response.FundamentalDTO;
import tw.com.stockplatform.fundamental.repository.StockFundamentalMapper;
import tw.com.stockplatform.fundamental.service.impl.FundamentalServiceImpl;
import tw.com.stockplatform.infrastructure.client.mops.MOPSClient;
import tw.com.stockplatform.infrastructure.client.mops.MOPSEpsDTO;
import tw.com.stockplatform.infrastructure.client.mops.MOPSFinancialSummaryDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * FundamentalServiceImpl 單元測試（Wave 2 Round 1 更新）。
 * <p>
 * B-BE-W2-03 修正驗證：確保最新季度取自 max(year, quarter)，而非 list.get(0)。
 */
@ExtendWith(MockitoExtension.class)
class FundamentalServiceImplTest {

    @Mock
    private StockFundamentalMapper fundamentalMapper;

    @Mock
    private MOPSClient mopsClient;

    @Mock
    private FundamentalConvertor fundamentalConvertor;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private FundamentalServiceImpl fundamentalService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("getFundamental：Redis cache hit 時直接回傳 DTO")
    void getFundamental_CacheHit() {
        FundamentalDTO cachedDto = buildFundamentalDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(cachedDto);

        FundamentalDTO result = fundamentalService.getFundamental(new FundamentalGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
    }

    @Test
    @DisplayName("getFundamental：DB 有資料時回傳 DB 資料")
    void getFundamental_DbHit() {
        StockFundamentalPO po = buildFundamentalPO("2330");
        FundamentalDTO dto = buildFundamentalDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(fundamentalMapper.findByStockId("2330")).thenReturn(Optional.of(po));
        when(fundamentalConvertor.toDTO(po)).thenReturn(dto);

        FundamentalDTO result = fundamentalService.getFundamental(new FundamentalGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        assertThat(result.eps()).isEqualByComparingTo("30.00");
        verify(valueOperations).set(anyString(), eq(dto), anyLong(), eq(SECONDS));
    }

    @Test
    @DisplayName("getFundamental：DB 無資料時呼叫 MOPS Client，帶入 market 參數")
    void getFundamental_FetchFromMOPS_withMarketParam() {
        FundamentalDTO dto = buildFundamentalDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(fundamentalMapper.findByStockId("2330")).thenReturn(Optional.empty());
        when(mopsClient.fetchEps(eq("2330"), eq("TWSE"))).thenReturn(List.of(
            new MOPSEpsDTO("2330", 2024, 1, new BigDecimal("8.00")),
            new MOPSEpsDTO("2330", 2023, 4, new BigDecimal("9.00"))
        ));
        when(mopsClient.fetchFinancialSummary(eq("2330"), eq("TWSE"))).thenReturn(
            new MOPSFinancialSummaryDTO("2330", new BigDecimal("25.0"), new BigDecimal("5.0"), new BigDecimal("28.0"))
        );
        when(fundamentalMapper.upsert(any())).thenReturn(1);
        when(fundamentalConvertor.toDTO(any())).thenReturn(dto);

        FundamentalDTO result = fundamentalService.getFundamental(new FundamentalGetRequest("2330"));

        assertThat(result).isNotNull();
        verify(mopsClient).fetchEps("2330", "TWSE");
        verify(mopsClient).fetchFinancialSummary("2330", "TWSE");
        verify(fundamentalMapper).upsert(any(StockFundamentalPO.class));
    }

    @Test
    @DisplayName("B-BE-W2-03 修正：EPS list 未依序排列時，reportYear/Quarter 仍應取最新季")
    void getFundamental_EpsUnsorted_CorrectLatestQuarter() {
        // Given：epsList 故意亂序（最新一季放在中間）
        // 2024 Q2 > 2024 Q1 > 2023 Q4
        List<MOPSEpsDTO> epsList = List.of(
            new MOPSEpsDTO("2330", 2023, 4, new BigDecimal("7.50")),  // 舊季
            new MOPSEpsDTO("2330", 2024, 2, new BigDecimal("10.00")), // 最新
            new MOPSEpsDTO("2330", 2024, 1, new BigDecimal("8.00"))   // 次新
        );
        when(valueOperations.get(anyString())).thenReturn(null);
        when(fundamentalMapper.findByStockId("2330")).thenReturn(Optional.empty());
        when(mopsClient.fetchEps(eq("2330"), anyString())).thenReturn(epsList);
        when(mopsClient.fetchFinancialSummary(eq("2330"), anyString())).thenReturn(
            new MOPSFinancialSummaryDTO("2330", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(fundamentalMapper.upsert(any())).thenReturn(1);
        when(fundamentalConvertor.toDTO(any())).thenReturn(buildFundamentalDTO("2330"));

        // Capture the PO saved to DB
        ArgumentCaptor<StockFundamentalPO> poCaptor = ArgumentCaptor.forClass(StockFundamentalPO.class);

        fundamentalService.getFundamental(new FundamentalGetRequest("2330"));

        verify(fundamentalMapper).upsert(poCaptor.capture());
        StockFundamentalPO savedPo = poCaptor.getValue();

        // 正確取 2024 Q2（最新），而非 2023 Q4（index=0）
        assertThat(savedPo.getReportYear()).isEqualTo(2024);
        assertThat(savedPo.getReportQuarter()).isEqualTo(2);
    }

    @Test
    @DisplayName("B-BE-W2-04：OTC 股票（4000-8999）應呼叫 fetchEps with market=OTC")
    void getFundamental_OtcStock_UsesOtcMarket() {
        String otcStockId = "6488"; // 環球晶，OTC
        FundamentalDTO dto = buildFundamentalDTO(otcStockId);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(fundamentalMapper.findByStockId(otcStockId)).thenReturn(Optional.empty());
        when(mopsClient.fetchEps(eq(otcStockId), eq("OTC"))).thenReturn(List.of());
        when(mopsClient.fetchFinancialSummary(eq(otcStockId), eq("OTC"))).thenReturn(
            new MOPSFinancialSummaryDTO(otcStockId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(fundamentalMapper.upsert(any())).thenReturn(1);
        when(fundamentalConvertor.toDTO(any())).thenReturn(dto);

        fundamentalService.getFundamental(new FundamentalGetRequest(otcStockId));

        verify(mopsClient).fetchEps(otcStockId, "OTC");
        verify(mopsClient).fetchFinancialSummary(otcStockId, "OTC");
    }

    // --- 輔助方法 ---

    private FundamentalDTO buildFundamentalDTO(String stockId) {
        return new FundamentalDTO(stockId, "台積電",
            new BigDecimal("30.00"), new BigDecimal("25.0"),
            new BigDecimal("5.0"), new BigDecimal("28.0"),
            2024, 1);
    }

    private StockFundamentalPO buildFundamentalPO(String stockId) {
        return StockFundamentalPO.builder()
            .fundamentalId("uuid-1")
            .stockId(stockId)
            .stockName("台積電")
            .eps(new BigDecimal("30.00"))
            .perRatio(new BigDecimal("25.0"))
            .pbrRatio(new BigDecimal("5.0"))
            .roe(new BigDecimal("28.0"))
            .reportYear(2024)
            .reportQuarter(1)
            .build();
    }
}

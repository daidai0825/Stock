package tw.com.stockplatform.fundamental.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import java.time.LocalDateTime;
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
 * FundamentalServiceImpl 單元測試（Wave 2 Round 2 更新）。
 * <p>
 * Round 2 變更：FundamentalDTO 結構改為 schema-lock v1.0
 * （per/pbr 去掉 Ratio 字尾 + source="MOPS" + updatedAt）。
 */
@ExtendWith(MockitoExtension.class)
// fundamentalConvertor_ToDTO_CorrectFieldMapping 直接 new Convertor，不依賴 mock，
// 使共用的 setUp() Redis stub 變成 unnecessary stubbing。採 LENIENT 允許共用 stub。
@MockitoSettings(strictness = Strictness.LENIENT)
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
    @DisplayName("getFundamental：DB 有資料時回傳 DB 資料（per/pbr 欄位名正確）")
    void getFundamental_DbHit() {
        StockFundamentalPO po = buildFundamentalPO("2330");
        FundamentalDTO dto = buildFundamentalDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(fundamentalMapper.findByStockId("2330")).thenReturn(Optional.of(po));
        when(fundamentalConvertor.toDTO(po)).thenReturn(dto);

        FundamentalDTO result = fundamentalService.getFundamental(new FundamentalGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        assertThat(result.eps()).isEqualByComparingTo("30.00");
        // 驗證 schema-lock 欄位名（per/pbr 不是 perRatio/pbrRatio）
        assertThat(result.per()).isEqualByComparingTo("25.0");
        assertThat(result.pbr()).isEqualByComparingTo("5.0");
        assertThat(result.source()).isEqualTo("MOPS");
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
        // 故意亂序：最新一季（2024 Q2）放在中間
        List<MOPSEpsDTO> epsList = List.of(
            new MOPSEpsDTO("2330", 2023, 4, new BigDecimal("7.50")),
            new MOPSEpsDTO("2330", 2024, 2, new BigDecimal("10.00")),
            new MOPSEpsDTO("2330", 2024, 1, new BigDecimal("8.00"))
        );
        when(valueOperations.get(anyString())).thenReturn(null);
        when(fundamentalMapper.findByStockId("2330")).thenReturn(Optional.empty());
        when(mopsClient.fetchEps(eq("2330"), anyString())).thenReturn(epsList);
        when(mopsClient.fetchFinancialSummary(eq("2330"), anyString())).thenReturn(
            new MOPSFinancialSummaryDTO("2330", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(fundamentalMapper.upsert(any())).thenReturn(1);
        when(fundamentalConvertor.toDTO(any())).thenReturn(buildFundamentalDTO("2330"));

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
        String otcStockId = "6488";
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

    @Test
    @DisplayName("FundamentalConvertor.toDTO：perRatio→per、pbrRatio→pbr、source=MOPS（直接驗證 Convertor 邏輯）")
    void fundamentalConvertor_ToDTO_CorrectFieldMapping() {
        // 不 mock Convertor，直接驗證 default toDTO 邏輯
        tw.com.stockplatform.fundamental.convertor.FundamentalConvertor convertor =
            new tw.com.stockplatform.fundamental.convertor.FundamentalConvertor() {};

        StockFundamentalPO po = buildFundamentalPO("2330");
        FundamentalDTO dto = convertor.toDTO(po);

        // 驗證欄位名映射正確
        assertThat(dto.per()).isEqualByComparingTo("25.0");   // perRatio → per
        assertThat(dto.pbr()).isEqualByComparingTo("5.0");    // pbrRatio → pbr
        assertThat(dto.source()).isEqualTo("MOPS");            // 固定值
        assertThat(dto.updatedAt()).isNotNull();
    }

    // --- 輔助方法 ---

    private FundamentalDTO buildFundamentalDTO(String stockId) {
        return new FundamentalDTO(
            stockId, "台積電",
            new BigDecimal("30.00"),
            new BigDecimal("25.0"),    // per（非 perRatio）
            new BigDecimal("5.0"),     // pbr（非 pbrRatio）
            new BigDecimal("28.0"),
            2024, 1,
            LocalDateTime.now(),
            "MOPS"
        );
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
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

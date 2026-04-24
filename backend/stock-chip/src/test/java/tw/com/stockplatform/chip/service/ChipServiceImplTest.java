package tw.com.stockplatform.chip.service;

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
import tw.com.stockplatform.chip.convertor.ChipConvertor;
import tw.com.stockplatform.chip.dto.request.ChipGetRequest;
import tw.com.stockplatform.chip.dto.response.ChipDTO;
import tw.com.stockplatform.chip.dto.response.InstitutionItem;
import tw.com.stockplatform.chip.repository.StockChipMapper;
import tw.com.stockplatform.chip.service.impl.ChipServiceImpl;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trading.TradingCalendarService;
import tw.com.stockplatform.domain.po.StockChipPO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEClient;
import tw.com.stockplatform.infrastructure.client.twse.TWSEInstitutionalDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * ChipServiceImpl 單元測試（Wave 2 Round 2 更新）。
 * <p>
 * Round 2 變更：ChipDTO 結構改為 schema-lock v1.0（institutions 陣列 + date + source + totalNetBuySell）。
 */
@ExtendWith(MockitoExtension.class)
// 部分測試（NotFound / FetchFromTWSE / Convertor 直測）會因 production 短路或不依賴 mock
// 而讓共用 setUp() 的 redisTemplate stub 變成 unnecessary stubbing。
// 採 LENIENT 允許共用 stub；個別測試仍透過 verify 驗證互動次數。
@MockitoSettings(strictness = Strictness.LENIENT)
class ChipServiceImplTest {

    @Mock
    private StockChipMapper chipMapper;

    @Mock
    private TWSEClient twseClient;

    @Mock
    private ChipConvertor chipConvertor;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private TradingCalendarService tradingCalendarService;

    @InjectMocks
    private ChipServiceImpl chipService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("getChip：Redis cache hit 時直接回傳 DTO，不呼叫 DB")
    void getChip_CacheHit() {
        ChipDTO cachedDto = buildChipDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(cachedDto);

        ChipDTO result = chipService.getChip(new ChipGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        verify(chipMapper, never()).findLatestByStockId(anyString());
    }

    @Test
    @DisplayName("getChip：DB 有今日資料且在有效交易日範圍內 → 直接回 DB")
    void getChip_DbHit_today() {
        LocalDate today = LocalDate.now();
        StockChipPO po = buildChipPO("2330", today);
        ChipDTO dto = buildChipDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(chipMapper.findLatestByStockId("2330")).thenReturn(Optional.of(po));
        // 同一 stub 內混用具體值與 matcher 會觸發 InvalidUseOfMatchers，需全部 matcher 化。
        when(tradingCalendarService.isValidRecentTradingDay(eq(today), any())).thenReturn(true);
        when(chipConvertor.toDTO(po)).thenReturn(dto);

        ChipDTO result = chipService.getChip(new ChipGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        verify(valueOperations).set(anyString(), eq(dto), anyLong(), eq(SECONDS));
    }

    @Test
    @DisplayName("getChip（B-BE-W2-02）：週末查詢，DB 有前交易日資料，在有效範圍內 → 回 DB，不呼叫外部")
    void getChip_Weekend_DbWithinRecentTradingDays_ReturnDbDirectly() {
        LocalDate friday = LocalDate.of(2026, 4, 17);
        StockChipPO po = buildChipPO("2330", friday);
        ChipDTO dto = buildChipDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(chipMapper.findLatestByStockId("2330")).thenReturn(Optional.of(po));
        // matcher 化避免 InvalidUseOfMatchers
        when(tradingCalendarService.isValidRecentTradingDay(eq(friday), any())).thenReturn(true);
        when(chipConvertor.toDTO(po)).thenReturn(dto);

        ChipDTO result = chipService.getChip(new ChipGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
        verify(twseClient, never()).fetchInstitutional(any());
    }

    @Test
    @DisplayName("getChip（B-BE-W2-02）：外部回空，DB 有最新一筆 → fallback DB（不拋例外）")
    void getChip_ExternalEmpty_FallbackToDb() {
        LocalDate friday = LocalDate.of(2026, 4, 17);
        StockChipPO po = buildChipPO("2330", friday);
        ChipDTO dto = buildChipDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(chipMapper.findLatestByStockId("2330")).thenReturn(Optional.of(po));
        // matcher 化避免 InvalidUseOfMatchers
        when(tradingCalendarService.isValidRecentTradingDay(eq(friday), any())).thenReturn(false);
        when(twseClient.fetchInstitutional(any())).thenReturn(List.of());
        when(chipConvertor.toDTO(po)).thenReturn(dto);

        ChipDTO result = chipService.getChip(new ChipGetRequest("2330"));

        assertThat(result.stockId()).isEqualTo("2330");
    }

    @Test
    @DisplayName("getChip：DB 無資料 + 外部無對應股票 → 拋 BusinessException（STOCK_NOT_FOUND）")
    void getChip_NotFound() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(chipMapper.findLatestByStockId("9999")).thenReturn(Optional.empty());
        when(tradingCalendarService.isValidRecentTradingDay(any(), any())).thenReturn(false);
        when(twseClient.fetchInstitutional(any())).thenReturn(List.of());

        assertThatThrownBy(() -> chipService.getChip(new ChipGetRequest("9999")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getChip：DB 無資料時呼叫 TWSE Client 並存入 DB")
    void getChip_FetchFromTWSE() {
        TWSEInstitutionalDTO twseDto = buildTWSEInstitutionalDto("2330");
        ChipDTO dto = buildChipDTO("2330");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(chipMapper.findLatestByStockId("2330")).thenReturn(Optional.empty());
        when(tradingCalendarService.isValidRecentTradingDay(any(), any())).thenReturn(false);
        when(twseClient.fetchInstitutional(any())).thenReturn(List.of(twseDto));
        when(chipMapper.upsert(any())).thenReturn(1);
        when(chipConvertor.toDTO(any())).thenReturn(dto);

        ChipDTO result = chipService.getChip(new ChipGetRequest("2330"));

        assertThat(result).isNotNull();
        verify(twseClient).fetchInstitutional(any(LocalDate.class));
        verify(chipMapper).upsert(any(StockChipPO.class));
    }

    @Test
    @DisplayName("ChipConvertor.toDTO：PO 轉 DTO 應產生 institutions 陣列（外資/投信/自營商順序固定）")
    void chipConvertor_ToDTO_ProducesInstitutionsArray() {
        // 直接使用 lambda 建立 ChipConvertor，驗證 default toDTO 邏輯
        // （MapStruct 在 production 生成 ChipConvertorImpl，測試此 default 方法邏輯等同）
        tw.com.stockplatform.chip.convertor.ChipConvertor convertor =
            new tw.com.stockplatform.chip.convertor.ChipConvertor() {};

        StockChipPO po = buildChipPO("2330", LocalDate.now());
        ChipDTO dto = convertor.toDTO(po);

        assertThat(dto.institutions()).hasSize(3);
        assertThat(dto.institutions().get(0).name()).isEqualTo("外資");
        assertThat(dto.institutions().get(1).name()).isEqualTo("投信");
        assertThat(dto.institutions().get(2).name()).isEqualTo("自營商");
        assertThat(dto.institutions().get(0).netBuySell()).isEqualTo(5_000L);
        assertThat(dto.institutions().get(1).netBuySell()).isEqualTo(1_000L);
        assertThat(dto.institutions().get(2).netBuySell()).isEqualTo(500L);
        assertThat(dto.totalNetBuySell()).isEqualTo(6_500L);
        assertThat(dto.source()).isEqualTo("TWSE");
        assertThat(dto.date()).isEqualTo(LocalDate.now());
    }

    // --- 輔助方法 ---

    private ChipDTO buildChipDTO(String stockId) {
        return new ChipDTO(
            stockId,
            LocalDate.now(),
            List.of(
                new InstitutionItem("外資",   0L, 0L, 5_000L),
                new InstitutionItem("投信",   0L, 0L, 1_000L),
                new InstitutionItem("自營商", 0L, 0L, 500L)
            ),
            6_500L,
            "TWSE"
        );
    }

    private StockChipPO buildChipPO(String stockId, LocalDate tradeDate) {
        return StockChipPO.builder()
            .chipId("uuid-1")
            .stockId(stockId)
            .stockName("台積電")
            .market("TWSE")
            .tradeDate(tradeDate)
            .foreignNetShares(new BigDecimal("5000"))
            .investmentTrustNetShares(new BigDecimal("1000"))
            .dealerNetShares(new BigDecimal("500"))
            .totalInstitutionalNet(new BigDecimal("6500"))
            .build();
    }

    private TWSEInstitutionalDTO buildTWSEInstitutionalDto(String stockId) {
        return new TWSEInstitutionalDTO(
            stockId, "台積電", LocalDate.now(),
            new BigDecimal("6000"), new BigDecimal("1000"), new BigDecimal("5000"),
            new BigDecimal("1200"), new BigDecimal("200"), new BigDecimal("1000"),
            new BigDecimal("500")
        );
    }
}

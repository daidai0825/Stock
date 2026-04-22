package tw.com.stockplatform.chip.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.com.stockplatform.chip.convertor.ChipConvertor;
import tw.com.stockplatform.chip.dto.request.ChipGetRequest;
import tw.com.stockplatform.chip.dto.response.ChipDTO;
import tw.com.stockplatform.chip.repository.StockChipMapper;
import tw.com.stockplatform.chip.service.ChipService;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trading.TradingCalendarService;
import tw.com.stockplatform.common.util.TimeUtils;
import tw.com.stockplatform.domain.po.StockChipPO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEClient;
import tw.com.stockplatform.infrastructure.client.twse.TWSEInstitutionalDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * M-CHIP 籌碼服務實作。
 * <p>
 * 快取策略：籌碼資料日更，TTL 1 天。
 * Key 格式：{@code {prefix}chip:{stockId}:daily}
 * <p>
 * Fallback 策略（修正 B-BE-W2-02）：
 * <ol>
 *   <li>Cache hit → 直接回</li>
 *   <li>DB 有資料且屬有效交易日範圍（≤3 個工作日）→ 直接回</li>
 *   <li>外部 TWSE 有資料 → 存 DB + cache 回傳</li>
 *   <li>外部回空（週末/假日）→ 回 DB 最新一筆（isStale 語義由上層判斷）</li>
 *   <li>DB 無 + 外部無 → 拋 STOCK_NOT_FOUND</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChipServiceImpl implements ChipService {

    // N-03: key prefix 由設定注入；N-04: 常數化
    @org.springframework.beans.factory.annotation.Value("${stock.cache.key-prefix:}")
    private String cacheKeyPrefix;

    private static final String CACHE_KEY_SUFFIX = ":daily";
    private static final long TTL_SECONDS = 86400L; // 1 天

    private final StockChipMapper chipMapper;
    private final TWSEClient twseClient;
    private final ChipConvertor chipConvertor;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TradingCalendarService tradingCalendarService;

    private String chipCacheKey(String stockId) {
        return cacheKeyPrefix + "chip:" + stockId + CACHE_KEY_SUFFIX;
    }

    @Override
    @Transactional
    public ChipDTO getChip(ChipGetRequest request) {
        String cacheKey = chipCacheKey(request.stockId());

        // 1. Cache hit
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ChipDTO dto) {
            log.debug("ChipService.getChip cache HIT stockId={}", request.stockId());
            return dto;
        }

        LocalDate today = TimeUtils.today();
        Optional<StockChipPO> dbResult = chipMapper.findLatestByStockId(request.stockId());

        // 2. DB 有資料且在有效交易日範圍內 → 直接回（包含週末情境）
        if (dbResult.isPresent() && tradingCalendarService.isValidRecentTradingDay(dbResult.get().getTradeDate(), today)) {
            ChipDTO dto = chipConvertor.toDTO(dbResult.get());
            redisTemplate.opsForValue().set(cacheKey, dto, TTL_SECONDS, TimeUnit.SECONDS);
            return dto;
        }

        // 3. 外部 TWSE 抓取
        ChipDTO externalDto = fetchFromTwseAndSave(request.stockId(), today);
        if (externalDto != null) {
            redisTemplate.opsForValue().set(cacheKey, externalDto, TTL_SECONDS, TimeUnit.SECONDS);
            return externalDto;
        }

        // 4. 外部回空（週末/假日）→ fallback DB 最新一筆
        if (dbResult.isPresent()) {
            log.info("ChipService.getChip external empty, falling back to DB stale stockId={} date={}",
                request.stockId(), dbResult.get().getTradeDate());
            ChipDTO dto = chipConvertor.toDTO(dbResult.get());
            redisTemplate.opsForValue().set(cacheKey, dto, TTL_SECONDS, TimeUnit.SECONDS);
            return dto;
        }

        // 5. DB 無 + 外部無
        throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
    }

    // --- 私有方法 ---

    /**
     * 從 TWSE 抓取三大法人買賣超並儲存。
     *
     * @return ChipDTO，若外部回空（週末/假日）則回 null
     */
    private ChipDTO fetchFromTwseAndSave(String stockId, LocalDate date) {
        List<TWSEInstitutionalDTO> institutionalList = twseClient.fetchInstitutional(date);

        return institutionalList.stream()
            .filter(dto -> stockId.equals(dto.stockId()))
            .findFirst()
            .map(match -> {
                BigDecimal totalNet = match.foreignNetShares()
                    .add(match.investmentTrustNetShares())
                    .add(match.dealerNetShares());

                StockChipPO po = StockChipPO.builder()
                    .chipId(UUID.randomUUID().toString())
                    .stockId(match.stockId())
                    .stockName(match.stockName())
                    .market("TWSE")
                    .tradeDate(match.tradeDate())
                    .foreignNetShares(match.foreignNetShares())
                    .investmentTrustNetShares(match.investmentTrustNetShares())
                    .dealerNetShares(match.dealerNetShares())
                    .totalInstitutionalNet(totalNet)
                    .createdAt(TimeUtils.now())
                    .build();

                chipMapper.upsert(po);
                return chipConvertor.toDTO(po);
            })
            .orElse(null); // 外部無此股票（週末/假日 TWSE 回空清單）
    }
}

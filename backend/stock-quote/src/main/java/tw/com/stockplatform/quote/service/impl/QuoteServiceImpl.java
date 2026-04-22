package tw.com.stockplatform.quote.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trading.TradingCalendarService;
import tw.com.stockplatform.common.util.TimeUtils;
import tw.com.stockplatform.domain.po.StockQuotePO;
import tw.com.stockplatform.infrastructure.client.otc.OTCClient;
import tw.com.stockplatform.infrastructure.client.otc.OTCDailyQuoteDTO;
import tw.com.stockplatform.infrastructure.client.otc.OTCOhlcDTO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEClient;
import tw.com.stockplatform.infrastructure.client.twse.TWSEDailyQuoteDTO;
import tw.com.stockplatform.infrastructure.client.twse.TWSEOhlcDTO;
import tw.com.stockplatform.quote.convertor.QuoteConvertor;
import tw.com.stockplatform.quote.dto.request.QuoteGetRequest;
import tw.com.stockplatform.quote.dto.request.QuoteHistoryRequest;
import tw.com.stockplatform.quote.dto.request.QuoteListRequest;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;
import tw.com.stockplatform.quote.repository.StockQuoteMapper;
import tw.com.stockplatform.quote.service.QuoteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * M-QUOTE 行情服務實作。
 * <p>
 * 快取策略：
 * <ul>
 *   <li>盤中（{@link #MARKET_OPEN}–{@link #MARKET_CLOSE}）TTL 30s</li>
 *   <li>盤後 TTL 1h，行情已固定</li>
 * </ul>
 * Key 格式：{@code {prefix}quote:{stockId}:daily}
 * <p>
 * Fallback 策略（修正 B-BE-W2-01）：
 * <ol>
 *   <li>Cache hit → 直接回</li>
 *   <li>DB 有最新資料且屬有效交易日範圍 → 回 DB（isStale 依日期是否為今日決定）</li>
 *   <li>外部成功 → 存 DB + cache，回外部資料（isStale=false）</li>
 *   <li>外部回空（週末/假日）→ 回 DB 最新一筆 + isStale=true</li>
 *   <li>DB 無 + 外部無 → 拋 STOCK_NOT_FOUND</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteServiceImpl implements QuoteService {

    // N-04: 抽常數，避免魔法數字
    static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    static final LocalTime MARKET_CLOSE = LocalTime.of(14, 30);

    private static final String CACHE_KEY_SUFFIX_DAILY = ":daily";
    private static final String CACHE_KEY_SUFFIX_HISTORY = ":history:";
    private static final long TTL_INTRADAY_SECONDS = 30L;
    private static final long TTL_AFTER_MARKET_SECONDS = 3600L;

    private final StockQuoteMapper quoteMapper;
    private final TWSEClient twseClient;
    private final OTCClient otcClient;
    private final QuoteConvertor quoteConvertor;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TradingCalendarService tradingCalendarService;

    // N-03: cache key prefix 改由 Spring 注入（application.yml: stock.cache.key-prefix）
    // 目前預設空字串，Wave 2.1 搭配設定項補完；已在 TD-5 追蹤
    @org.springframework.beans.factory.annotation.Value("${stock.cache.key-prefix:}")
    private String cacheKeyPrefix;

    private String quoteCacheKey(String stockId) {
        return cacheKeyPrefix + "quote:" + stockId + CACHE_KEY_SUFFIX_DAILY;
    }

    private String historyCacheKey(String stockId, int year, int month) {
        return cacheKeyPrefix + "quote:" + stockId + CACHE_KEY_SUFFIX_HISTORY + year + "-" + month;
    }

    @Override
    @Transactional
    public QuoteDTO getQuote(QuoteGetRequest request) {
        String cacheKey = quoteCacheKey(request.stockId());

        // 1. Cache hit
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof QuoteDTO dto) {
            log.debug("QuoteService.getQuote cache HIT stockId={}", request.stockId());
            return dto;
        }

        LocalDate today = TimeUtils.today();

        // 2. DB：有資料且屬有效交易日範圍（≤3 個工作日）→ 直接回
        Optional<StockQuotePO> dbResult = quoteMapper.findLatestByStockId(request.stockId());
        if (dbResult.isPresent()) {
            StockQuotePO po = dbResult.get();
            if (tradingCalendarService.isValidRecentTradingDay(po.getQuoteDate(), today)) {
                // 若是今日資料 isStale=false；若是前一個交易日（非今日）isStale=true
                boolean stale = !today.equals(po.getQuoteDate());
                QuoteDTO dto = stale ? quoteConvertor.toDTOStale(po) : quoteConvertor.toDTO(po);
                putCache(cacheKey, dto);
                return dto;
            }
        }

        // 3. 外部取最新行情
        QuoteDTO externalDto = fetchFromExternalAndSave(request.stockId(), today);
        if (externalDto != null) {
            putCache(cacheKey, externalDto);
            return externalDto;
        }

        // 4. 外部回空（週末/假日）→ fallback DB 最新一筆 + isStale=true
        if (dbResult.isPresent()) {
            log.info("QuoteService.getQuote external empty, falling back to DB stale stockId={} date={}",
                request.stockId(), dbResult.get().getQuoteDate());
            QuoteDTO staleDto = quoteConvertor.toDTOStale(dbResult.get());
            putCache(cacheKey, staleDto);
            return staleDto;
        }

        // 5. DB 無 + 外部無
        throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
    }

    @Override
    @Transactional
    public List<QuoteDTO> listQuotes(QuoteListRequest request) {
        List<String> stockIds = request.stockIds();
        List<QuoteDTO> result = new ArrayList<>();
        List<String> missIds = new ArrayList<>();

        // 1. 批次 cache lookup
        for (String stockId : stockIds) {
            String cacheKey = quoteCacheKey(stockId);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof QuoteDTO dto) {
                result.add(dto);
            } else {
                missIds.add(stockId);
            }
        }
        if (missIds.isEmpty()) {
            return result;
        }

        // 2. DB 批次查詢 cache miss
        LocalDate today = TimeUtils.today();
        List<StockQuotePO> dbRows = quoteMapper.findLatestByStockIds(missIds);
        List<String> stillMissIds = new ArrayList<>(missIds);

        for (StockQuotePO po : dbRows) {
            boolean stale = !today.equals(po.getQuoteDate());
            QuoteDTO dto = stale ? quoteConvertor.toDTOStale(po) : quoteConvertor.toDTO(po);
            result.add(dto);
            putCache(quoteCacheKey(po.getStockId()), dto);
            stillMissIds.remove(po.getStockId());
        }

        if (stillMissIds.isEmpty()) {
            return result;
        }

        // 3. M-BE-W2-07: DB 仍 miss → 批次抓取全市場，再過濾（非串行逐一呼叫）
        List<QuoteDTO> fetched = fetchAndCacheAll(today);
        for (QuoteDTO dto : fetched) {
            if (stillMissIds.contains(dto.stockId())) {
                result.add(dto);
                stillMissIds.remove(dto.stockId());
            }
        }

        // stillMissIds 剩餘的在外部也查無，略過（不拋例外，允許部分失敗）
        if (!stillMissIds.isEmpty()) {
            log.warn("QuoteService.listQuotes: {} stocks not found externally: {}", stillMissIds.size(), stillMissIds);
        }
        return result;
    }

    @Override
    @Transactional
    public List<QuoteDTO> getHistory(QuoteHistoryRequest request) {
        String cacheKey = historyCacheKey(request.stockId(),
            request.month().getYear(), request.month().getMonthValue());

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            log.debug("QuoteService.getHistory cache HIT stockId={}", request.stockId());
            @SuppressWarnings("unchecked")
            List<QuoteDTO> dtoList = (List<QuoteDTO>) list;
            return dtoList;
        }

        LocalDate startDate = request.month().withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 1. DB 查詢
        List<StockQuotePO> dbRows = quoteMapper.findHistoryByStockId(request.stockId(), startDate, endDate);
        if (!dbRows.isEmpty()) {
            List<QuoteDTO> dtoList = dbRows.stream().map(quoteConvertor::toDTO).toList();
            putHistoryCache(cacheKey, dtoList);
            return dtoList;
        }

        // 2. 外部補抓（TWSE 優先）
        List<TWSEOhlcDTO> ohlcList = twseClient.fetchStockHistory(request.stockId(), request.month());
        if (ohlcList.isEmpty()) {
            List<OTCOhlcDTO> otcList = otcClient.fetchStockHistory(request.stockId(), request.month());
            LocalDateTime now = TimeUtils.now();
            List<QuoteDTO> dtoList = otcList.stream()
                .map(dto -> {
                    StockQuotePO po = StockQuotePO.builder()
                        .quoteId(UUID.randomUUID().toString())
                        .stockId(dto.stockId())
                        .stockName("")
                        .market("OTC")
                        .openPrice(dto.openPrice())
                        .highPrice(dto.highPrice())
                        .lowPrice(dto.lowPrice())
                        .closePrice(dto.closePrice())
                        .volume(dto.volume())
                        .quoteDate(dto.tradeDate())
                        .createdAt(now)
                        .build();
                    quoteMapper.upsert(po);
                    return quoteConvertor.toDTO(po);
                })
                .toList();
            putHistoryCache(cacheKey, dtoList);
            return dtoList;
        }

        LocalDateTime now = TimeUtils.now();
        List<QuoteDTO> dtoList = ohlcList.stream()
            .map(dto -> {
                StockQuotePO po = quoteConvertor.fromTWSEOhlc(dto, "", now);
                quoteMapper.upsert(po);
                return quoteConvertor.toDTO(po);
            })
            .toList();
        putHistoryCache(cacheKey, dtoList);
        return dtoList;
    }

    // --- 私有輔助方法 ---

    /**
     * 從外部取得個股當日行情並儲存。
     *
     * @return QuoteDTO，若外部回空（週末/假日）則回 null
     */
    private QuoteDTO fetchFromExternalAndSave(String stockId, LocalDate date) {
        LocalDateTime now = TimeUtils.now();
        List<TWSEDailyQuoteDTO> twseRows = twseClient.fetchDailyQuotes(date).stream()
            .filter(dto -> stockId.equals(dto.stockId()))
            .toList();

        if (!twseRows.isEmpty()) {
            StockQuotePO po = quoteConvertor.fromTWSEDaily(twseRows.get(0), now);
            quoteMapper.upsert(po);
            return quoteConvertor.toDTO(po);
        }

        List<OTCDailyQuoteDTO> otcRows = otcClient.fetchDailyQuotes(date).stream()
            .filter(dto -> stockId.equals(dto.stockId()))
            .toList();

        if (!otcRows.isEmpty()) {
            StockQuotePO po = quoteConvertor.fromOTCDaily(otcRows.get(0), now);
            quoteMapper.upsert(po);
            return quoteConvertor.toDTO(po);
        }

        // 外部回空（週末/假日/下市）
        return null;
    }

    /**
     * M-BE-W2-07: 一次性抓取全市場行情（TWSE + OTC），存入 DB + Cache，並回傳 DTO list。
     * <p>
     * 取代原本 stillMissIds 的逐一呼叫，避免 N 次外部 API 呼叫。
     */
    private List<QuoteDTO> fetchAndCacheAll(LocalDate date) {
        LocalDateTime now = TimeUtils.now();
        List<QuoteDTO> fetched = new ArrayList<>();

        // TWSE
        try {
            List<TWSEDailyQuoteDTO> twseList = twseClient.fetchDailyQuotes(date);
            for (TWSEDailyQuoteDTO dto : twseList) {
                StockQuotePO po = quoteConvertor.fromTWSEDaily(dto, now);
                quoteMapper.upsert(po);
                QuoteDTO quoteDTO = quoteConvertor.toDTO(po);
                putCache(quoteCacheKey(po.getStockId()), quoteDTO);
                fetched.add(quoteDTO);
            }
            log.info("QuoteService.fetchAndCacheAll TWSE fetched count={} date={}", twseList.size(), date);
        } catch (Exception ex) {
            log.warn("QuoteService.fetchAndCacheAll TWSE failed date={}", date, ex);
        }

        // OTC
        try {
            List<OTCDailyQuoteDTO> otcList = otcClient.fetchDailyQuotes(date);
            for (OTCDailyQuoteDTO dto : otcList) {
                StockQuotePO po = quoteConvertor.fromOTCDaily(dto, now);
                quoteMapper.upsert(po);
                QuoteDTO quoteDTO = quoteConvertor.toDTO(po);
                putCache(quoteCacheKey(po.getStockId()), quoteDTO);
                fetched.add(quoteDTO);
            }
            log.info("QuoteService.fetchAndCacheAll OTC fetched count={} date={}", otcList.size(), date);
        } catch (Exception ex) {
            log.warn("QuoteService.fetchAndCacheAll OTC failed date={}", date, ex);
        }

        return fetched;
    }

    private void putCache(String key, QuoteDTO dto) {
        long ttl = isMarketHours() ? TTL_INTRADAY_SECONDS : TTL_AFTER_MARKET_SECONDS;
        redisTemplate.opsForValue().set(key, dto, ttl, TimeUnit.SECONDS);
    }

    private void putHistoryCache(String key, List<QuoteDTO> dtoList) {
        redisTemplate.opsForValue().set(key, dtoList, TTL_AFTER_MARKET_SECONDS, TimeUnit.SECONDS);
    }

    private boolean isMarketHours() {
        LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Taipei")).toLocalTime();
        return !now.isBefore(MARKET_OPEN) && !now.isAfter(MARKET_CLOSE);
    }
}

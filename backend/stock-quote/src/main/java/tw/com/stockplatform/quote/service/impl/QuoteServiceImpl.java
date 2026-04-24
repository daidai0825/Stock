package tw.com.stockplatform.quote.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import tw.com.stockplatform.quote.dto.response.HistoryItem;
import tw.com.stockplatform.quote.dto.response.QuoteDTO;
import tw.com.stockplatform.quote.dto.response.QuoteHistoryResponse;
import tw.com.stockplatform.quote.enums.KLinePeriod;
import tw.com.stockplatform.quote.repository.StockQuoteMapper;
import tw.com.stockplatform.quote.service.QuoteService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * M-QUOTE 行情服務實作（schema-lock v1.0 Round 2 對齊）。
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
 * <p>
 * 週期聚合邏輯（Round 2 新增）：
 * <ul>
 *   <li>daily：直接回 DB 日線資料</li>
 *   <li>weekly：依自然週（週一至週五）彙總</li>
 *   <li>monthly：依自然月彙總</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteServiceImpl implements QuoteService {

    // N-04: 抽常數，避免魔法數字
    static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    static final LocalTime MARKET_CLOSE = LocalTime.of(14, 30);

    // schema-lock §3.4 預設查詢範圍
    private static final int DEFAULT_DAILY_DAYS = 90;
    private static final int DEFAULT_WEEKLY_DAYS = 365;
    private static final int DEFAULT_MONTHLY_DAYS = 365 * 5;

    // schema-lock §3.4 最長查詢範圍
    private static final long MAX_DAILY_DAYS = 1825L;   // 5 年
    private static final long MAX_WEEKLY_DAYS = 1825L;  // 5 年
    private static final long MAX_MONTHLY_DAYS = 3650L; // 10 年

    private static final String CACHE_KEY_SUFFIX_DAILY = ":daily";
    private static final long TTL_INTRADAY_SECONDS = 30L;
    private static final long TTL_AFTER_MARKET_SECONDS = 3600L;
    private static final long TTL_HISTORY_SECONDS = 3600L;

    private final StockQuoteMapper quoteMapper;
    private final TWSEClient twseClient;
    private final OTCClient otcClient;
    private final QuoteConvertor quoteConvertor;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TradingCalendarService tradingCalendarService;

    // N-03: cache key prefix 改由 Spring 注入（application.yml: stock.cache.key-prefix）
    @Value("${stock.cache.key-prefix:}")
    private String cacheKeyPrefix;

    private String quoteCacheKey(String stockId) {
        return cacheKeyPrefix + "quote:" + stockId + CACHE_KEY_SUFFIX_DAILY;
    }

    private String historyCacheKey(String stockId, KLinePeriod period, LocalDate startDate, LocalDate endDate) {
        return cacheKeyPrefix + "quote:" + stockId + ":history:" + period + ":" + startDate + ":" + endDate;
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
                boolean stale = !today.equals(po.getQuoteDate());
                BigDecimal previousClose = resolvePreviousClose(request.stockId(), po.getQuoteDate());
                QuoteDTO dto = stale
                    ? quoteConvertor.toDTOStale(po, previousClose)
                    : quoteConvertor.toDTO(po, previousClose);
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
            BigDecimal previousClose = resolvePreviousClose(request.stockId(), dbResult.get().getQuoteDate());
            QuoteDTO staleDto = quoteConvertor.toDTOStale(dbResult.get(), previousClose);
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
            return applyMarketFilter(result, request.market());
        }

        // 2. DB 批次查詢 cache miss
        LocalDate today = TimeUtils.today();
        List<StockQuotePO> dbRows = quoteMapper.findLatestByStockIds(missIds);
        List<String> stillMissIds = new ArrayList<>(missIds);

        for (StockQuotePO po : dbRows) {
            boolean stale = !today.equals(po.getQuoteDate());
            BigDecimal previousClose = resolvePreviousClose(po.getStockId(), po.getQuoteDate());
            QuoteDTO dto = stale
                ? quoteConvertor.toDTOStale(po, previousClose)
                : quoteConvertor.toDTO(po, previousClose);
            result.add(dto);
            putCache(quoteCacheKey(po.getStockId()), dto);
            stillMissIds.remove(po.getStockId());
        }

        if (stillMissIds.isEmpty()) {
            return applyMarketFilter(result, request.market());
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
        return applyMarketFilter(result, request.market());
    }

    @Override
    @Transactional
    public QuoteHistoryResponse getHistory(QuoteHistoryRequest request) {
        // 參數驗證：預設日期範圍
        LocalDate today = TimeUtils.today();
        LocalDate endDate = request.endDate() != null ? request.endDate() : today;
        LocalDate startDate = request.startDate() != null
            ? request.startDate()
            : resolveDefaultStartDate(request.period(), endDate);

        // schema-lock §3.4：startDate > endDate → 1002
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_INVALID);
        }

        // schema-lock §3.4：超出最長範圍 → 1003
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        validateRange(request.period(), daysBetween);

        String cacheKey = historyCacheKey(request.stockId(), request.period(), startDate, endDate);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof QuoteHistoryResponse resp) {
            log.debug("QuoteService.getHistory cache HIT stockId={} period={}", request.stockId(), request.period());
            return resp;
        }

        // 1. DB 查詢日線資料
        List<StockQuotePO> dbRows = quoteMapper.findHistoryByStockId(request.stockId(), startDate, endDate);
        if (dbRows.isEmpty()) {
            // 2. 外部補抓（TWSE 優先，僅 daily 支援外部補抓；weekly/monthly 依賴 DB 日線聚合）
            dbRows = fetchHistoryFromExternal(request.stockId(), startDate, endDate);
        }

        List<HistoryItem> items = aggregateHistory(dbRows, request.period());
        QuoteHistoryResponse response = new QuoteHistoryResponse(request.stockId(), request.period(), items);
        redisTemplate.opsForValue().set(cacheKey, response, TTL_HISTORY_SECONDS, TimeUnit.SECONDS);
        return response;
    }

    // --- 私有輔助方法 ---

    /**
     * 查詢前一交易日收盤價（用於 change/changePercent 計算）。
     * 若查無前一日，回 null（Convertor 處理為 change=0）。
     */
    private BigDecimal resolvePreviousClose(String stockId, LocalDate quoteDate) {
        return quoteMapper.findPreviousByStockId(stockId, quoteDate)
            .map(StockQuotePO::getClosePrice)
            .orElse(null);
    }

    /**
     * M-BE-W2-05 實作：market 欄位過濾。
     * market 為 null 或 "ALL" 時不過濾。
     */
    private List<QuoteDTO> applyMarketFilter(List<QuoteDTO> dtoList, String market) {
        if (market == null || "ALL".equalsIgnoreCase(market)) {
            return dtoList;
        }
        return dtoList.stream()
            .filter(dto -> market.equalsIgnoreCase(dto.market()))
            .toList();
    }

    /**
     * 依 period 計算預設 startDate。
     */
    private LocalDate resolveDefaultStartDate(KLinePeriod period, LocalDate endDate) {
        return switch (period) {
            case daily -> endDate.minusDays(DEFAULT_DAILY_DAYS);
            case weekly -> endDate.minusDays(DEFAULT_WEEKLY_DAYS);
            case monthly -> endDate.minusDays(DEFAULT_MONTHLY_DAYS);
        };
    }

    /**
     * 驗證查詢範圍是否超出最長限制。
     */
    private void validateRange(KLinePeriod period, long daysBetween) {
        long maxDays = switch (period) {
            case daily -> MAX_DAILY_DAYS;
            case weekly -> MAX_WEEKLY_DAYS;
            case monthly -> MAX_MONTHLY_DAYS;
        };
        if (daysBetween > maxDays) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE);
        }
    }

    /**
     * 將日線 PO 列表依 period 聚合為 HistoryItem 列表。
     * <p>
     * daily：直接轉換（1 PO = 1 HistoryItem）。
     * weekly：依自然週（以週一為起始）彙總：
     *   - date = 週首個交易日
     *   - open = 週首個交易日 open
     *   - high = 週最高 high
     *   - low = 週最低 low
     *   - close = 週最後一個交易日 close
     *   - volume = 週成交量加總
     * monthly：以自然月彙總，規則同上。
     */
    private List<HistoryItem> aggregateHistory(List<StockQuotePO> rows, KLinePeriod period) {
        if (rows.isEmpty()) {
            return List.of();
        }
        // 確保升冪排序
        List<StockQuotePO> sorted = rows.stream()
            .sorted(Comparator.comparing(StockQuotePO::getQuoteDate))
            .toList();

        return switch (period) {
            case daily -> sorted.stream()
                .map(quoteConvertor::toHistoryItem)
                .toList();
            case weekly -> aggregateByKey(sorted, this::weekKey);
            case monthly -> aggregateByKey(sorted, this::monthKey);
        };
    }

    /**
     * 依 keyExtractor 分組聚合。
     * 使用 LinkedHashMap 保持插入順序（升冪）。
     */
    private List<HistoryItem> aggregateByKey(List<StockQuotePO> sorted,
                                              Function<LocalDate, LocalDate> keyExtractor) {
        // key = 週/月的代表日（週一 / 月第一天），value = 該組所有 PO
        Map<LocalDate, List<StockQuotePO>> grouped = new LinkedHashMap<>();
        for (StockQuotePO po : sorted) {
            LocalDate key = keyExtractor.apply(po.getQuoteDate());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(po);
        }

        List<HistoryItem> items = new ArrayList<>();
        for (Map.Entry<LocalDate, List<StockQuotePO>> entry : grouped.entrySet()) {
            List<StockQuotePO> group = entry.getValue();
            // 取首/末
            StockQuotePO first = group.get(0);
            StockQuotePO last = group.get(group.size() - 1);

            BigDecimal high = group.stream()
                .map(StockQuotePO::getHighPrice)
                .filter(v -> v != null)
                .max(BigDecimal::compareTo)
                .orElse(null);
            BigDecimal low = group.stream()
                .map(StockQuotePO::getLowPrice)
                .filter(v -> v != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
            long totalVolume = group.stream()
                .mapToLong(po -> po.getVolume() != null ? po.getVolume() : 0L)
                .sum();

            items.add(new HistoryItem(
                first.getQuoteDate(),                                              // date = 首個交易日
                first.getOpenPrice() != null ? first.getOpenPrice().setScale(2, RoundingMode.HALF_UP) : null,
                high != null ? high.setScale(2, RoundingMode.HALF_UP) : null,
                low != null ? low.setScale(2, RoundingMode.HALF_UP) : null,
                last.getClosePrice() != null ? last.getClosePrice().setScale(2, RoundingMode.HALF_UP) : null,
                totalVolume
            ));
        }
        return items;
    }

    /**
     * 週 key = 該週週一（自然週起始）。
     */
    private LocalDate weekKey(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 月 key = 該月第一天。
     */
    private LocalDate monthKey(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * 從外部補抓歷史資料（TWSE 優先），存入 DB 後回傳 PO 列表。
     * 注意：外部 API 限定日 K，weekly/monthly 聚合依賴此資料。
     */
    private List<StockQuotePO> fetchHistoryFromExternal(String stockId, LocalDate startDate, LocalDate endDate) {
        List<StockQuotePO> result = new ArrayList<>();
        LocalDateTime now = TimeUtils.now();

        // 外部 history API 以月為單位查詢，逐月補抓（TWSE 優先）
        LocalDate cursor = startDate.withDayOfMonth(1);
        while (!cursor.isAfter(endDate)) {
            List<TWSEOhlcDTO> ohlcList = twseClient.fetchStockHistory(stockId, cursor);
            if (ohlcList.isEmpty()) {
                // 嘗試 OTC
                List<OTCOhlcDTO> otcList = otcClient.fetchStockHistory(stockId, cursor);
                for (OTCOhlcDTO dto : otcList) {
                    if (!dto.tradeDate().isBefore(startDate) && !dto.tradeDate().isAfter(endDate)) {
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
                        result.add(po);
                    }
                }
            } else {
                for (TWSEOhlcDTO dto : ohlcList) {
                    if (!dto.tradeDate().isBefore(startDate) && !dto.tradeDate().isAfter(endDate)) {
                        StockQuotePO po = quoteConvertor.fromTWSEOhlc(dto, "", now);
                        quoteMapper.upsert(po);
                        result.add(po);
                    }
                }
            }
            cursor = cursor.plusMonths(1);
        }
        return result.stream()
            .sorted(Comparator.comparing(StockQuotePO::getQuoteDate))
            .toList();
    }

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
            BigDecimal previousClose = resolvePreviousClose(stockId, po.getQuoteDate());
            return quoteConvertor.toDTO(po, previousClose);
        }

        List<OTCDailyQuoteDTO> otcRows = otcClient.fetchDailyQuotes(date).stream()
            .filter(dto -> stockId.equals(dto.stockId()))
            .toList();

        if (!otcRows.isEmpty()) {
            StockQuotePO po = quoteConvertor.fromOTCDaily(otcRows.get(0), now);
            quoteMapper.upsert(po);
            BigDecimal previousClose = resolvePreviousClose(stockId, po.getQuoteDate());
            return quoteConvertor.toDTO(po, previousClose);
        }

        // 外部回空（週末/假日/下市）
        return null;
    }

    /**
     * M-BE-W2-07: 一次性抓取全市場行情（TWSE + OTC），存入 DB + Cache，並回傳 DTO list。
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
                BigDecimal previousClose = resolvePreviousClose(po.getStockId(), po.getQuoteDate());
                QuoteDTO quoteDTO = quoteConvertor.toDTO(po, previousClose);
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
                BigDecimal previousClose = resolvePreviousClose(po.getStockId(), po.getQuoteDate());
                QuoteDTO quoteDTO = quoteConvertor.toDTO(po, previousClose);
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

    private boolean isMarketHours() {
        LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Taipei")).toLocalTime();
        return !now.isBefore(MARKET_OPEN) && !now.isAfter(MARKET_CLOSE);
    }
}

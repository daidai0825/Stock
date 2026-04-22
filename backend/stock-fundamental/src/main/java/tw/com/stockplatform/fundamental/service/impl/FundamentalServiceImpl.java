package tw.com.stockplatform.fundamental.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.com.stockplatform.common.util.TimeUtils;
import tw.com.stockplatform.domain.po.StockFundamentalPO;
import tw.com.stockplatform.fundamental.convertor.FundamentalConvertor;
import tw.com.stockplatform.fundamental.dto.request.FundamentalGetRequest;
import tw.com.stockplatform.fundamental.dto.response.FundamentalDTO;
import tw.com.stockplatform.fundamental.repository.StockFundamentalMapper;
import tw.com.stockplatform.fundamental.service.FundamentalService;
import tw.com.stockplatform.infrastructure.client.mops.MOPSClient;
import tw.com.stockplatform.infrastructure.client.mops.MOPSEpsDTO;
import tw.com.stockplatform.infrastructure.client.mops.MOPSFinancialSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * M-FUND 基本面服務實作。
 * <p>
 * 快取策略：基本面變動慢，TTL 1 天。
 * Key 格式：{@code {prefix}fundamental:{stockId}}
 * <p>
 * B-BE-W2-03 修正：取最新季度使用 stream().max() 而非 get(0)，確保不依賴排序結果。
 * B-BE-W2-04 修正：呼叫 MOPSClient 時帶入 market 參數決定 TYPEK（sii/otc）。
 * <p>
 * Wave 2 short-term: market 從 StockFundamentalPO 取得（若 DB 已有）；
 * 若首次抓取，預設 "TWSE"。Wave 3 (TD-7) 補充 stock_info 表查詢。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundamentalServiceImpl implements FundamentalService {

    // N-03: key prefix 由設定注入
    @org.springframework.beans.factory.annotation.Value("${stock.cache.key-prefix:}")
    private String cacheKeyPrefix;

    private static final long TTL_SECONDS = 86400L; // 1 天

    private final StockFundamentalMapper fundamentalMapper;
    private final MOPSClient mopsClient;
    private final FundamentalConvertor fundamentalConvertor;
    private final RedisTemplate<String, Object> redisTemplate;

    private String fundamentalCacheKey(String stockId) {
        return cacheKeyPrefix + "fundamental:" + stockId;
    }

    @Override
    @Transactional
    public FundamentalDTO getFundamental(FundamentalGetRequest request) {
        String cacheKey = fundamentalCacheKey(request.stockId());

        // 1. Cache hit
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof FundamentalDTO dto) {
            log.debug("FundamentalService.getFundamental cache HIT stockId={}", request.stockId());
            return dto;
        }

        // 2. DB
        var dbResult = fundamentalMapper.findByStockId(request.stockId());
        if (dbResult.isPresent()) {
            FundamentalDTO dto = fundamentalConvertor.toDTO(dbResult.get());
            redisTemplate.opsForValue().set(cacheKey, dto, TTL_SECONDS, TimeUnit.SECONDS);
            return dto;
        }

        // 3. 外部 MOPS 抓取
        // B-BE-W2-04: market 參數決定 TYPEK
        // Wave 3 TODO (TD-7): 從 stock_info 表查詢 market 欄位
        String market = resolveMarket(request.stockId());
        FundamentalDTO dto = fetchFromMopsAndSave(request.stockId(), market);
        redisTemplate.opsForValue().set(cacheKey, dto, TTL_SECONDS, TimeUnit.SECONDS);
        return dto;
    }

    // --- 私有方法 ---

    private FundamentalDTO fetchFromMopsAndSave(String stockId, String market) {
        LocalDateTime now = TimeUtils.now();

        // B-BE-W2-04: 傳入 market 參數
        List<MOPSEpsDTO> epsList = mopsClient.fetchEps(stockId, market);
        MOPSFinancialSummaryDTO summary = mopsClient.fetchFinancialSummary(stockId, market);

        // 近四季 EPS 加總
        BigDecimal totalEps = epsList.stream()
            .sorted(Comparator.comparingInt(MOPSEpsDTO::year)
                .thenComparingInt(MOPSEpsDTO::quarter).reversed())
            .limit(4)
            .map(MOPSEpsDTO::eps)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // B-BE-W2-03 修正：用 max() 取最新季度，不依賴 list 排序
        var latest = epsList.stream()
            .max(Comparator.comparingInt(MOPSEpsDTO::year).thenComparingInt(MOPSEpsDTO::quarter));
        int reportYear = latest.map(MOPSEpsDTO::year).orElse(0);
        int reportQuarter = latest.map(MOPSEpsDTO::quarter).orElse(0);

        StockFundamentalPO po = StockFundamentalPO.builder()
            .fundamentalId(UUID.randomUUID().toString())
            .stockId(stockId)
            .stockName("")
            .eps(totalEps)
            .perRatio(summary.perRatio())
            .pbrRatio(summary.pbrRatio())
            .roe(summary.roe())
            .reportYear(reportYear)
            .reportQuarter(reportQuarter)
            .updatedAt(now)
            .createdAt(now)
            .build();

        fundamentalMapper.upsert(po);
        return fundamentalConvertor.toDTO(po);
    }

    /**
     * 推斷股票市場別。
     * <p>
     * Wave 2 短期策略：OTC 股票代號通常為 4 位數字且大於等於 4000 且小於等於 9999，
     * 或以字母開頭（如 006208 ETF 另算）。此為暫行規則，不保證 100% 正確。
     * Wave 3 (TD-7) 應從 stock_info.market 欄位查詢。
     *
     * @param stockId 股票代號
     * @return "OTC" 或 "TWSE"
     */
    private String resolveMarket(String stockId) {
        // Wave 3 TODO: 從 stock_info 表查 market 欄位
        // 目前暫行規則：依股號長度/範圍簡易判斷（不精確，列 TD）
        if (stockId != null && stockId.matches("\\d+")) {
            int code = Integer.parseInt(stockId);
            // OTC 股票代號範圍：4 碼 4000-8999（粗略判斷）
            if (code >= 4000 && code <= 8999) {
                log.debug("FundamentalService.resolveMarket stockId={} inferred market=OTC (wave2 heuristic)", stockId);
                return "OTC";
            }
        }
        return "TWSE";
    }
}

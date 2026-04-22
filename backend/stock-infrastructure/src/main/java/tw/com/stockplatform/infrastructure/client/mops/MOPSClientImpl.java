package tw.com.stockplatform.infrastructure.client.mops;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.ExternalServiceException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MOPS Client 實作。
 * <p>
 * MOPS 採 form POST 查詢，回傳 HTML 或 JSON（依 enctype）。
 * 本實作呼叫 MOPS JSON API（需帶 token=xxx 參數）。
 * <br>
 * 設計決策：MOPS 官方無公開純 JSON API，此處呼叫其半公開 JSON endpoint；
 * 若 API 格式異動，需更新 parseFn。基本面變動慢，TTL 設 1 天（由 FundamentalService 快取）。
 * <p>
 * B-BE-W2-04 修正：
 * <ul>
 *   <li>market 參數決定 TYPEK（TWSE→sii / OTC→otc）</li>
 *   <li>co_id 使用 URLEncoder.encode 避免特殊字元問題</li>
 *   <li>解析前檢查必要欄位；格式異動時拋 MOPS_FORMAT_CHANGED（5014）</li>
 * </ul>
 */
@Slf4j
@Component
public class MOPSClientImpl implements MOPSClient {

    private static final String EPS_ENDPOINT = "/mops/web/ajax_t163sb04";
    private static final String FINANCIAL_ENDPOINT = "/mops/web/ajax_t163sb05";

    /** TWSE 上市 → sii；OTC 上櫃 → otc。 */
    private static final String TYPEK_TWSE = "sii";
    private static final String TYPEK_OTC = "otc";

    /** 允許的市場別（防禦性）。 */
    private static final Set<String> ALLOWED_MARKETS = Set.of("TWSE", "OTC");

    private final RestClient restClient;

    public MOPSClientImpl(@Qualifier("mopsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<MOPSEpsDTO> fetchEps(String stockId, String market) {
        long start = System.currentTimeMillis();
        String typek = resolveTypek(market);
        String encodedId = URLEncoder.encode(stockId, StandardCharsets.UTF_8);
        String body = "co_id=" + encodedId + "&TYPEK=" + typek + "&isnew=false";

        try {
            log.info("MOPS fetchEps stockId={} market={} typek={} traceId={}",
                stockId, market, typek, MDC.get("traceId"));
            JsonNode root = restClient.post()
                .uri(EPS_ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            List<MOPSEpsDTO> result = parseEps(root, stockId);
            log.info("MOPS fetchEps completed stockId={} count={} elapsed={}ms",
                stockId, result.size(), elapsed(start));
            return result;
        } catch (ExternalServiceException ex) {
            throw ex; // 已分類的例外直接往上拋（供 @Retryable 處理）
        } catch (RestClientException ex) {
            log.error("MOPS fetchEps network error stockId={} elapsed={}ms traceId={}",
                stockId, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.MOPS_DATA_SOURCE_ERROR, ex);
        }
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public MOPSFinancialSummaryDTO fetchFinancialSummary(String stockId, String market) {
        long start = System.currentTimeMillis();
        String typek = resolveTypek(market);
        String encodedId = URLEncoder.encode(stockId, StandardCharsets.UTF_8);
        String body = "co_id=" + encodedId + "&TYPEK=" + typek + "&isnew=false";

        try {
            log.info("MOPS fetchFinancialSummary stockId={} market={} typek={} traceId={}",
                stockId, market, typek, MDC.get("traceId"));
            JsonNode root = restClient.post()
                .uri(FINANCIAL_ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            MOPSFinancialSummaryDTO result = parseFinancialSummary(root, stockId);
            log.info("MOPS fetchFinancialSummary completed stockId={} elapsed={}ms",
                stockId, elapsed(start));
            return result;
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("MOPS fetchFinancialSummary network error stockId={} elapsed={}ms traceId={}",
                stockId, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.MOPS_DATA_SOURCE_ERROR, ex);
        }
    }

    // --- 私有解析方法 ---

    private List<MOPSEpsDTO> parseEps(JsonNode root, String stockId) {
        List<MOPSEpsDTO> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        JsonNode dataNode = root.isArray() ? root : root.path("data");
        if (!dataNode.isArray()) {
            return result;
        }
        for (JsonNode row : dataNode) {
            // M-BE-W2-08: 解析前檢查必要欄位
            if (!hasRequiredFields(row, "year", "season", "eps")) {
                log.warn("MOPS fetchEps unexpected schema: missing required fields. stockId={}", stockId);
                throw new ExternalServiceException(ErrorCode.MOPS_FORMAT_CHANGED,
                    new RuntimeException("MOPS EPS response missing required fields"));
            }
            try {
                int year = parseYear(row.path("year").asText());
                int quarter = parseQuarter(row.path("season").asText());
                BigDecimal eps = parseBD(row.path("eps").asText());
                result.add(new MOPSEpsDTO(stockId, year, quarter, eps));
            } catch (ExternalServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                log.debug("MOPS parseEps skip row stockId={}", stockId);
            }
        }
        return result;
    }

    private MOPSFinancialSummaryDTO parseFinancialSummary(JsonNode root, String stockId) {
        if (root == null) {
            return emptyFinancialSummary(stockId);
        }
        JsonNode dataNode = root.isArray() ? root.get(0) : root.path("data").get(0);
        if (dataNode == null) {
            return emptyFinancialSummary(stockId);
        }
        // M-BE-W2-08: 解析前檢查必要欄位
        if (!hasRequiredFields(dataNode, "per", "pbr", "roe")) {
            log.warn("MOPS fetchFinancialSummary unexpected schema: missing per/pbr/roe. stockId={}", stockId);
            throw new ExternalServiceException(ErrorCode.MOPS_FORMAT_CHANGED,
                new RuntimeException("MOPS FinancialSummary response missing required fields"));
        }
        return new MOPSFinancialSummaryDTO(
            stockId,
            parseBD(dataNode.path("per").asText()),
            parseBD(dataNode.path("pbr").asText()),
            parseBD(dataNode.path("roe").asText())
        );
    }

    private MOPSFinancialSummaryDTO emptyFinancialSummary(String stockId) {
        return new MOPSFinancialSummaryDTO(stockId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** 判斷 market 對應的 TYPEK。未知市場預設 sii 並記錄 warn。 */
    private String resolveTypek(String market) {
        if ("OTC".equalsIgnoreCase(market)) {
            return TYPEK_OTC;
        }
        if (market != null && !ALLOWED_MARKETS.contains(market.toUpperCase())) {
            log.warn("MOPS resolveTypek: unknown market={}, defaulting to sii", market);
        }
        return TYPEK_TWSE;
    }

    /** 檢查 JSON 節點是否含所有指定欄位（不允許 missing/null）。 */
    private boolean hasRequiredFields(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.path(field).isMissingNode()) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal parseBD(String raw) {
        if (raw == null || raw.isBlank() || "--".equals(raw.trim()) || "N/A".equalsIgnoreCase(raw.trim())) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(raw.replaceAll(",", "").trim());
    }

    private int parseYear(String raw) {
        // MOPS 民國年轉西元年
        return Integer.parseInt(raw.trim()) + 1911;
    }

    private int parseQuarter(String raw) {
        return switch (raw.trim()) {
            case "Q1", "1" -> 1;
            case "Q2", "2" -> 2;
            case "Q3", "3" -> 3;
            case "Q4", "4" -> 4;
            default -> 0;
        };
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

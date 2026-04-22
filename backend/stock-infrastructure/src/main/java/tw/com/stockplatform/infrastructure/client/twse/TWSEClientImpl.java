package tw.com.stockplatform.infrastructure.client.twse;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * TWSE Client 實作。
 * <p>
 * 呼叫 TWSE 公開 JSON API，失敗時 @Retryable 自動重試最多 3 次（指數退避 1→2→4s）。
 * <br>
 * 設計決策：TWSE API 回傳格式為 {"stat":"OK","data":[[...],...]}，
 * 欄位順序固定，依索引取值（不依賴欄位名稱，避免 API 改名影響）。
 */
@Slf4j
@Component
public class TWSEClientImpl implements TWSEClient {

    private static final DateTimeFormatter TWSE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String TWSE_DAILY_ENDPOINT = "/exchangeReport/MI_INDEX";
    private static final String TWSE_STOCK_HISTORY_ENDPOINT = "/exchangeReport/STOCK_DAY";
    private static final String TWSE_INSTITUTIONAL_ENDPOINT = "/fund/T86";

    private final RestClient restClient;

    public TWSEClientImpl(@Qualifier("twseRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<TWSEDailyQuoteDTO> fetchDailyQuotes(LocalDate date) {
        String endpoint = TWSE_DAILY_ENDPOINT + "?response=json&date=" + date.format(TWSE_DATE_FMT) + "&type=ALLBUT0999";
        long start = System.currentTimeMillis();
        try {
            log.info("TWSE fetchDailyQuotes endpoint={} traceId={}", endpoint, MDC.get("traceId"));
            JsonNode root = restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(JsonNode.class);

            List<TWSEDailyQuoteDTO> result = parseDailyQuotes(root, date);
            log.info("TWSE fetchDailyQuotes completed count={} elapsed={}ms", result.size(), elapsed(start));
            return result;
        } catch (RestClientException ex) {
            log.error("TWSE fetchDailyQuotes failed endpoint={} elapsed={}ms traceId={}",
                endpoint, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.TWSE_DATA_SOURCE_ERROR, ex);
        }
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<TWSEOhlcDTO> fetchStockHistory(String stockId, LocalDate date) {
        String endpoint = TWSE_STOCK_HISTORY_ENDPOINT
            + "?response=json&date=" + date.format(TWSE_DATE_FMT) + "&stockNo=" + stockId;
        long start = System.currentTimeMillis();
        try {
            log.info("TWSE fetchStockHistory stockId={} endpoint={} traceId={}", stockId, endpoint, MDC.get("traceId"));
            JsonNode root = restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(JsonNode.class);

            List<TWSEOhlcDTO> result = parseStockHistory(root, stockId);
            log.info("TWSE fetchStockHistory completed stockId={} count={} elapsed={}ms", stockId, result.size(), elapsed(start));
            return result;
        } catch (RestClientException ex) {
            log.error("TWSE fetchStockHistory failed stockId={} elapsed={}ms traceId={}",
                stockId, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.TWSE_DATA_SOURCE_ERROR, ex);
        }
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<TWSEInstitutionalDTO> fetchInstitutional(LocalDate date) {
        String endpoint = TWSE_INSTITUTIONAL_ENDPOINT + "?response=json&date=" + date.format(TWSE_DATE_FMT)
            + "&selectType=ALL";
        long start = System.currentTimeMillis();
        try {
            log.info("TWSE fetchInstitutional endpoint={} traceId={}", endpoint, MDC.get("traceId"));
            JsonNode root = restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(JsonNode.class);

            List<TWSEInstitutionalDTO> result = parseInstitutional(root, date);
            log.info("TWSE fetchInstitutional completed count={} elapsed={}ms", result.size(), elapsed(start));
            return result;
        } catch (RestClientException ex) {
            log.error("TWSE fetchInstitutional failed endpoint={} elapsed={}ms traceId={}",
                endpoint, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.TWSE_DATA_SOURCE_ERROR, ex);
        }
    }

    // --- 私有解析方法 ---

    private List<TWSEDailyQuoteDTO> parseDailyQuotes(JsonNode root, LocalDate date) {
        List<TWSEDailyQuoteDTO> result = new ArrayList<>();
        if (root == null || !isStatOk(root)) {
            return result;
        }
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            return result;
        }
        for (JsonNode row : dataNode) {
            if (row.size() < 9) {
                continue;
            }
            try {
                result.add(new TWSEDailyQuoteDTO(
                    cleanStr(row.get(0).asText()),
                    cleanStr(row.get(1).asText()),
                    date,
                    parseBD(row.get(4).asText()),
                    parseBD(row.get(5).asText()),
                    parseBD(row.get(6).asText()),
                    parseBD(row.get(7).asText()),
                    parseLong(row.get(2).asText())
                ));
            } catch (Exception ex) {
                log.debug("TWSE parseDailyQuotes skip row: {}", row);
            }
        }
        return result;
    }

    private List<TWSEOhlcDTO> parseStockHistory(JsonNode root, String stockId) {
        List<TWSEOhlcDTO> result = new ArrayList<>();
        if (root == null || !isStatOk(root)) {
            return result;
        }
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            return result;
        }
        DateTimeFormatter minguo = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        for (JsonNode row : dataNode) {
            if (row.size() < 7) {
                continue;
            }
            try {
                // TWSE 民國年轉西元
                String dateStr = convertMinguo(row.get(0).asText());
                result.add(new TWSEOhlcDTO(
                    stockId,
                    LocalDate.parse(dateStr, minguo),
                    parseBD(row.get(3).asText()),
                    parseBD(row.get(4).asText()),
                    parseBD(row.get(5).asText()),
                    parseBD(row.get(6).asText()),
                    parseLong(row.get(1).asText()),
                    parseLong(row.get(2).asText())
                ));
            } catch (Exception ex) {
                log.debug("TWSE parseStockHistory skip row: {}", row);
            }
        }
        return result;
    }

    private List<TWSEInstitutionalDTO> parseInstitutional(JsonNode root, LocalDate date) {
        List<TWSEInstitutionalDTO> result = new ArrayList<>();
        if (root == null || !isStatOk(root)) {
            return result;
        }
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            return result;
        }
        for (JsonNode row : dataNode) {
            if (row.size() < 12) {
                continue;
            }
            try {
                result.add(new TWSEInstitutionalDTO(
                    cleanStr(row.get(0).asText()),
                    cleanStr(row.get(1).asText()),
                    date,
                    parseBD(row.get(2).asText()),
                    parseBD(row.get(3).asText()),
                    parseBD(row.get(4).asText()),
                    parseBD(row.get(5).asText()),
                    parseBD(row.get(6).asText()),
                    parseBD(row.get(7).asText()),
                    parseBD(row.get(10).asText())
                ));
            } catch (Exception ex) {
                log.debug("TWSE parseInstitutional skip row: {}", row);
            }
        }
        return result;
    }

    private boolean isStatOk(JsonNode root) {
        JsonNode stat = root.path("stat");
        return !stat.isMissingNode() && "OK".equalsIgnoreCase(stat.asText());
    }

    private String cleanStr(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private BigDecimal parseBD(String raw) {
        if (raw == null || raw.isBlank() || "--".equals(raw.trim())) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(raw.replaceAll(",", "").trim());
    }

    private long parseLong(String raw) {
        if (raw == null || raw.isBlank() || "--".equals(raw.trim())) {
            return 0L;
        }
        return Long.parseLong(raw.replaceAll(",", "").trim());
    }

    private String convertMinguo(String minGuoDate) {
        // 民國 113/01/02 → 2024/01/02
        String[] parts = minGuoDate.trim().split("/");
        int westernYear = Integer.parseInt(parts[0]) + 1911;
        return westernYear + "/" + parts[1] + "/" + parts[2];
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

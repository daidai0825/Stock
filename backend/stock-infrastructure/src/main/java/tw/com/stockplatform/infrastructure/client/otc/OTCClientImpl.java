package tw.com.stockplatform.infrastructure.client.otc;

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
 * OTC（上櫃）Client 實作。
 * <p>
 * 呼叫 TPEX 公開 JSON API，失敗時自動重試最多 3 次。
 * <br>
 * TPEX API 日期格式為民國年，需轉換。
 */
@Slf4j
@Component
public class OTCClientImpl implements OTCClient {

    private static final String OTC_DAILY_ENDPOINT = "/openapi/v1/tpex_mainboard_close_quotes";
    private static final String OTC_HISTORY_ENDPOINT = "/openapi/v1/tpex_mainboard_daily_close_quotes";

    private final RestClient restClient;

    public OTCClientImpl(@Qualifier("otcRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<OTCDailyQuoteDTO> fetchDailyQuotes(LocalDate date) {
        // TPEX 日期格式：民國年/月/日，例如 113/01/02
        String minguoDate = toMinguo(date);
        String endpoint = OTC_DAILY_ENDPOINT + "?date=" + minguoDate;
        long start = System.currentTimeMillis();
        try {
            log.info("OTC fetchDailyQuotes endpoint={} traceId={}", endpoint, MDC.get("traceId"));
            JsonNode root = restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(JsonNode.class);

            List<OTCDailyQuoteDTO> result = parseDailyQuotes(root, date);
            log.info("OTC fetchDailyQuotes completed count={} elapsed={}ms", result.size(), elapsed(start));
            return result;
        } catch (RestClientException ex) {
            log.error("OTC fetchDailyQuotes failed endpoint={} elapsed={}ms traceId={}",
                endpoint, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.OTC_DATA_SOURCE_ERROR, ex);
        }
    }

    @Override
    @Retryable(
        retryFor = {RestClientException.class, ExternalServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<OTCOhlcDTO> fetchStockHistory(String stockId, LocalDate date) {
        String minguoDate = toMinguo(date);
        String endpoint = OTC_HISTORY_ENDPOINT + "?date=" + minguoDate + "&code=" + stockId;
        long start = System.currentTimeMillis();
        try {
            log.info("OTC fetchStockHistory stockId={} endpoint={} traceId={}", stockId, endpoint, MDC.get("traceId"));
            JsonNode root = restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(JsonNode.class);

            List<OTCOhlcDTO> result = parseStockHistory(root, stockId);
            log.info("OTC fetchStockHistory completed stockId={} count={} elapsed={}ms", stockId, result.size(), elapsed(start));
            return result;
        } catch (RestClientException ex) {
            log.error("OTC fetchStockHistory failed stockId={} elapsed={}ms traceId={}",
                stockId, elapsed(start), MDC.get("traceId"));
            throw new ExternalServiceException(ErrorCode.OTC_DATA_SOURCE_ERROR, ex);
        }
    }

    // --- 私有解析方法 ---

    private List<OTCDailyQuoteDTO> parseDailyQuotes(JsonNode root, LocalDate date) {
        List<OTCDailyQuoteDTO> result = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return result;
        }
        for (JsonNode item : root) {
            try {
                result.add(new OTCDailyQuoteDTO(
                    cleanStr(item.path("SecuritiesCompanyCode").asText()),
                    cleanStr(item.path("Company").asText()),
                    date,
                    parseBD(item.path("Open").asText()),
                    parseBD(item.path("High").asText()),
                    parseBD(item.path("Low").asText()),
                    parseBD(item.path("Close").asText()),
                    parseLong(item.path("TradingShares").asText())
                ));
            } catch (Exception ex) {
                log.debug("OTC parseDailyQuotes skip item: {}", item.path("SecuritiesCompanyCode").asText());
            }
        }
        return result;
    }

    private List<OTCOhlcDTO> parseStockHistory(JsonNode root, String stockId) {
        List<OTCOhlcDTO> result = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return result;
        }
        DateTimeFormatter minguo = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        for (JsonNode item : root) {
            try {
                String dateStr = convertMinguo(item.path("Date").asText());
                result.add(new OTCOhlcDTO(
                    stockId,
                    LocalDate.parse(dateStr, minguo),
                    parseBD(item.path("Open").asText()),
                    parseBD(item.path("High").asText()),
                    parseBD(item.path("Low").asText()),
                    parseBD(item.path("Close").asText()),
                    parseLong(item.path("Volume").asText())
                ));
            } catch (Exception ex) {
                log.debug("OTC parseStockHistory skip item");
            }
        }
        return result;
    }

    private String toMinguo(LocalDate date) {
        return (date.getYear() - 1911) + "/" + String.format("%02d", date.getMonthValue())
            + "/" + String.format("%02d", date.getDayOfMonth());
    }

    private String convertMinguo(String minGuoDate) {
        String[] parts = minGuoDate.trim().split("/");
        int westernYear = Integer.parseInt(parts[0]) + 1911;
        return westernYear + "/" + parts[1] + "/" + parts[2];
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

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}

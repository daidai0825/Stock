package tw.com.stockplatform.infrastructure.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 外部 Client 專用 RestClient 工廠設定。
 * <p>
 * M-BE-W2-04: 改用 Apache HttpClient 5 PoolingHttpClientConnectionManager，
 * 取代 SimpleClientHttpRequestFactory（每次呼叫重建 TCP 連線）。
 * <br>
 * connect 3s / read 10s；每 route 最多 20 連線，總計最多 50 連線。
 * <br>
 * 依 SSRF 防護原則各 Client 使用固定 baseUrl，不接受使用者輸入。
 * <p>
 * M-BE-W2-03: 移除 fallback，dev/prod 強制外部注入；local 已在 application-local.yml 設定。
 * <p>
 * B-01 修正（Spring 6.1+ 編譯 blocker）:
 * {@code HttpComponentsClientHttpRequestFactory.setReadTimeout(int)} 在 Spring Framework 6.1+
 * 已被移除。改用 Apache HttpClient 5 {@code RequestConfig.setResponseTimeout} 設定讀取逾時，
 * 行為與原 setReadTimeout 等效。
 */
@Configuration
public class RestClientConfig {

    private static final int MAX_CONN_TOTAL = 50;
    private static final int MAX_CONN_PER_ROUTE = 20;
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int RESPONSE_TIMEOUT_MS = 10_000;

    @Value("${stock.external.twse.base-url}")
    private String twseBaseUrl;

    @Value("${stock.external.otc.base-url}")
    private String otcBaseUrl;

    @Value("${stock.external.mops.base-url}")
    private String mopsBaseUrl;

    @Bean("twseRestClient")
    public RestClient twseRestClient() {
        return RestClient.builder()
            .baseUrl(twseBaseUrl)
            .requestFactory(buildRequestFactory())
            .build();
    }

    @Bean("otcRestClient")
    public RestClient otcRestClient() {
        return RestClient.builder()
            .baseUrl(otcBaseUrl)
            .requestFactory(buildRequestFactory())
            .build();
    }

    @Bean("mopsRestClient")
    public RestClient mopsRestClient() {
        return RestClient.builder()
            .baseUrl(mopsBaseUrl)
            .requestFactory(buildRequestFactory())
            .build();
    }

    /**
     * M-BE-W2-04: HttpClient 5 連線池工廠。
     * <p>
     * B-01: Spring 6.1+ 移除了 setReadTimeout(int)，改在 RequestConfig 層設定
     * responseTimeout（語義等同原 read timeout）。connectTimeout 仍透過
     * factory.setConnectTimeout 設定以保持一致性。
     */
    private HttpComponentsClientHttpRequestFactory buildRequestFactory() {
        HttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(MAX_CONN_TOTAL)
                .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                .setConnectionTimeToLive(TimeValue.ofSeconds(30))
                .build();

        // B-01: 以 RequestConfig 設定 responseTimeout，取代已移除的 setReadTimeout
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
            .setResponseTimeout(Timeout.ofMilliseconds(RESPONSE_TIMEOUT_MS))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(TimeValue.ofSeconds(60))
            .build();

        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        // 不再呼叫 factory.setReadTimeout()（Spring 6.1+ 已移除）
        return factory;
    }
}

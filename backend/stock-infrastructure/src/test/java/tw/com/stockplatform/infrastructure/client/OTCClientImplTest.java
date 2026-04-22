package tw.com.stockplatform.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tw.com.stockplatform.common.exception.ExternalServiceException;
import tw.com.stockplatform.infrastructure.client.otc.OTCClientImpl;
import tw.com.stockplatform.infrastructure.client.otc.OTCDailyQuoteDTO;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * OTCClientImpl 單元測試（mock RestClient）。
 */
@ExtendWith(MockitoExtension.class)
class OTCClientImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OTCClientImpl otcClient;

    @BeforeEach
    void setUp() {
        otcClient = new OTCClientImpl(restClient);
    }

    @Test
    @DisplayName("fetchDailyQuotes：OTC 回 JSON 陣列時正確解析行情")
    void fetchDailyQuotes_Success() {
        // Given
        ArrayNode mockResponse = buildDailyQuotesResponse();
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(mockResponse);

        // When
        List<OTCDailyQuoteDTO> result = otcClient.fetchDailyQuotes(LocalDate.of(2024, 1, 2));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).stockId()).isEqualTo("6789");
        assertThat(result.get(0).closePrice()).isEqualByComparingTo("123.50");
    }

    @Test
    @DisplayName("fetchDailyQuotes：OTC 回 null 時回空集合")
    void fetchDailyQuotes_NullResponse() {
        // Given
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(null);

        // When
        List<OTCDailyQuoteDTO> result = otcClient.fetchDailyQuotes(LocalDate.of(2024, 1, 2));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchDailyQuotes：RestClient 拋例外時包裝為 ExternalServiceException")
    void fetchDailyQuotes_Exception() {
        // Given
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class))
            .thenThrow(new RestClientException("timeout"));

        // Then
        assertThatThrownBy(() -> otcClient.fetchDailyQuotes(LocalDate.of(2024, 1, 2)))
            .isInstanceOf(ExternalServiceException.class);
    }

    private ArrayNode buildDailyQuotesResponse() {
        ArrayNode arr = MAPPER.createArrayNode();
        ObjectNode item = arr.addObject();
        item.put("SecuritiesCompanyCode", "6789");
        item.put("Company", "測試上櫃股");
        item.put("Open", "120.00");
        item.put("High", "125.00");
        item.put("Low", "119.50");
        item.put("Close", "123.50");
        item.put("TradingShares", "500,000");
        return arr;
    }
}

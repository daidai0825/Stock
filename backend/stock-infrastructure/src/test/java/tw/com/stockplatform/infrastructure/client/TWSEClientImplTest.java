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
import tw.com.stockplatform.infrastructure.client.twse.TWSEClientImpl;
import tw.com.stockplatform.infrastructure.client.twse.TWSEDailyQuoteDTO;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TWSEClientImpl 單元測試（mock RestClient）。
 */
@ExtendWith(MockitoExtension.class)
class TWSEClientImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private TWSEClientImpl twseClient;

    @BeforeEach
    void setUp() {
        twseClient = new TWSEClientImpl(restClient);
    }

    @Test
    @DisplayName("fetchDailyQuotes：TWSE 回 stat=OK 時正確解析行情")
    void fetchDailyQuotes_Success() {
        // Given
        ObjectNode mockResponse = buildDailyQuotesResponse();
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(mockResponse);

        // When
        List<TWSEDailyQuoteDTO> result = twseClient.fetchDailyQuotes(LocalDate.of(2024, 1, 2));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).stockId()).isEqualTo("2330");
        assertThat(result.get(0).closePrice()).isEqualByComparingTo("567.00");
    }

    @Test
    @DisplayName("fetchDailyQuotes：TWSE 回 stat != OK 時回空集合")
    void fetchDailyQuotes_StatNotOk() {
        // Given
        ObjectNode mockResponse = MAPPER.createObjectNode();
        mockResponse.put("stat", "FAIL");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(mockResponse);

        // When
        List<TWSEDailyQuoteDTO> result = twseClient.fetchDailyQuotes(LocalDate.of(2024, 1, 2));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchDailyQuotes：RestClient 拋例外時包裝為 ExternalServiceException")
    void fetchDailyQuotes_RestClientException() {
        // Given
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class))
            .thenThrow(new RestClientException("Connection timeout"));

        // Then
        assertThatThrownBy(() -> twseClient.fetchDailyQuotes(LocalDate.of(2024, 1, 2)))
            .isInstanceOf(ExternalServiceException.class);
    }

    // --- 輔助方法 ---

    private ObjectNode buildDailyQuotesResponse() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("stat", "OK");
        ArrayNode data = root.putArray("data");
        ArrayNode row = data.addArray();
        // 欄位 0:stockId, 1:name, 2:volume, 3:turnover, 4:open, 5:high, 6:low, 7:close, 8:change
        row.add("2330");
        row.add("台積電");
        row.add("10,000,000");
        row.add("5,670,000,000");
        row.add("560.00");
        row.add("570.00");
        row.add("558.00");
        row.add("567.00");
        row.add("+7.00");
        return root;
    }
}

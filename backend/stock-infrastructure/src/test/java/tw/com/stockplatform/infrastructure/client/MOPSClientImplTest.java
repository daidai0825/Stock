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
import tw.com.stockplatform.infrastructure.client.mops.MOPSClientImpl;
import tw.com.stockplatform.infrastructure.client.mops.MOPSEpsDTO;
import tw.com.stockplatform.infrastructure.client.mops.MOPSFinancialSummaryDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * MOPSClientImpl 單元測試（mock RestClient）。
 */
@ExtendWith(MockitoExtension.class)
class MOPSClientImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private MOPSClientImpl mopsClient;

    @BeforeEach
    void setUp() {
        mopsClient = new MOPSClientImpl(restClient);
    }

    @Test
    @DisplayName("fetchEps：MOPS 回 JSON 陣列時正確解析 EPS")
    void fetchEps_Success() {
        // Given
        ArrayNode mockResponse = buildEpsResponse();
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(mockResponse);

        // When
        List<MOPSEpsDTO> result = mopsClient.fetchEps("2330", "TWSE");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).stockId()).isEqualTo("2330");
        assertThat(result.get(0).eps()).isEqualByComparingTo("32.34");
    }

    @Test
    @DisplayName("fetchFinancialSummary：MOPS 回財務摘要時正確解析")
    void fetchFinancialSummary_Success() {
        // Given
        ArrayNode mockResponse = buildFinancialSummaryResponse();
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(mockResponse);

        // When
        MOPSFinancialSummaryDTO result = mopsClient.fetchFinancialSummary("2330", "TWSE");

        // Then
        assertThat(result.stockId()).isEqualTo("2330");
        assertThat(result.perRatio()).isEqualByComparingTo("25.50");
        assertThat(result.roe()).isEqualByComparingTo("28.30");
    }

    @Test
    @DisplayName("fetchEps：RestClient 拋例外時包裝為 ExternalServiceException")
    void fetchEps_Exception() {
        // Given
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.fasterxml.jackson.databind.JsonNode.class))
            .thenThrow(new RestClientException("Connection refused"));

        // Then
        assertThatThrownBy(() -> mopsClient.fetchEps("2330", "TWSE"))
            .isInstanceOf(ExternalServiceException.class);
    }

    private ArrayNode buildEpsResponse() {
        ArrayNode arr = MAPPER.createArrayNode();
        ObjectNode q1 = arr.addObject();
        q1.put("year", "112");
        q1.put("season", "Q4");
        q1.put("eps", "32.34");
        ObjectNode q2 = arr.addObject();
        q2.put("year", "112");
        q2.put("season", "Q3");
        q2.put("eps", "8.14");
        return arr;
    }

    private ArrayNode buildFinancialSummaryResponse() {
        ArrayNode arr = MAPPER.createArrayNode();
        ObjectNode item = arr.addObject();
        item.put("per", "25.50");
        item.put("pbr", "5.20");
        item.put("roe", "28.30");
        return arr;
    }
}

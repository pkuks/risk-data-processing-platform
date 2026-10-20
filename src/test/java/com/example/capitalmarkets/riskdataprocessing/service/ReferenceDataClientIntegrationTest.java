package com.example.capitalmarkets.riskdataprocessing.service;

import com.example.capitalmarkets.riskdataprocessing.model.ReferenceDataResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port=8082)
public class ReferenceDataClientIntegrationTest {
    @Autowired
    private ReferenceDataClient referenceDataClient;

    @Test
    void should_SuccessfullyMapAndRetrieveData_When_ServerReturnsValidJson(){
        stubFor(get(urlEqualTo("/api/reference-data/EQUITY"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "instrument": "EQUITY",
                                    "valuationPrice": 155.00,
                                    "currency": "USD"
                                }
                                """)));

        ReferenceDataResponse response = referenceDataClient.getReferenceData("EQUITY");

        assertThat(response).isNotNull();
        assertThat(response.instrument()).isEqualTo("EQUITY");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.valuationPrice()).isEqualByComparingTo("155.00");

    }

    @Test
    void should_ThrowRestClientException_When_ServerReturnsErrorStatusCode() {
        // Arrange: Stub a down or misconfigured route
        stubFor(get(urlEqualTo("/api/reference-data/UNKNOWN"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        // Act & Assert: Verify that RestClient's default error handling bubbles up correctly
        assertThatThrownBy(() -> referenceDataClient.getReferenceData("UNKNOWN"))
                .isInstanceOf(RestClientException.class);
    }
}

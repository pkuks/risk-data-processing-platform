package com.example.capitalmarkets.riskdataprocessing.service;

import com.example.capitalmarkets.riskdataprocessing.model.ReferenceDataResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReferenceDataClient {
    private final RestClient restClient;

    public ReferenceDataClient(
            RestClient.Builder builder,
            @Value("${reference-data.service.base-url}") String baseUrl
    ){
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ReferenceDataResponse getReferenceData(String instrument){
        return restClient.get()
                .uri("/api/reference-data/{instrument}", instrument)
                .retrieve()
                .body(ReferenceDataResponse.class);
    }
}

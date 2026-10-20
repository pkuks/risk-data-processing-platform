package com.example.capitalmarkets.riskdataprocessing.model;

import java.math.BigDecimal;

public record ReferenceDataResponse(
        String instrument,
        BigDecimal valuationPrice,
        String currency
) {
}

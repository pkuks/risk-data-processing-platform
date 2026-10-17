package com.example.capitalmarkets.riskdataprocessing.model;

import java.math.BigDecimal;

public record TradeInput(
        String tradeId,
        String instrument,
        String currency,
        BigDecimal quantity,
        BigDecimal price
) {
}

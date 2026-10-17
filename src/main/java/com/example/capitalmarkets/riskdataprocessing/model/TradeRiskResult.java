package com.example.capitalmarkets.riskdataprocessing.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeRiskResult(
        String tradeId,
        String instrument,
        String currency,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal exposure,
        LocalDate businessDate
) {
}

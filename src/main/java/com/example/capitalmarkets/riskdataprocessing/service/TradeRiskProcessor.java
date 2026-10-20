package com.example.capitalmarkets.riskdataprocessing.service;

import com.example.capitalmarkets.riskdataprocessing.model.ReferenceDataResponse;
import com.example.capitalmarkets.riskdataprocessing.model.TradeInput;
import com.example.capitalmarkets.riskdataprocessing.model.TradeRiskResult;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TradeRiskProcessor implements ItemProcessor<TradeInput, TradeRiskResult> {

    private final LocalDate businessDate;
    private final ReferenceDataClient referenceDataClient;
    public TradeRiskProcessor(LocalDate businessDate, ReferenceDataClient referenceDataClient) {
        this.businessDate = businessDate;
        this.referenceDataClient = referenceDataClient;
    }

    @Override
    public TradeRiskResult process(TradeInput trade) throws Exception {
        if (trade.price()==null || trade.price().compareTo(BigDecimal.ZERO)<0){
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (trade.quantity() == null || trade.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        //BigDecimal exposure = trade.quantity().multiply(trade.price());
        ReferenceDataResponse refData = referenceDataClient.getReferenceData(trade.instrument());

        if (refData == null || refData.valuationPrice() == null) {
            throw new IllegalArgumentException("Missing market valuation price for instrument: " + trade.instrument());
        }
        BigDecimal calculatedExposure = trade.quantity().multiply(refData.valuationPrice());
        return new TradeRiskResult(
                trade.tradeId(), trade.instrument(), trade.currency(),
                trade.quantity(),trade.price(), calculatedExposure, businessDate
        );    }
}

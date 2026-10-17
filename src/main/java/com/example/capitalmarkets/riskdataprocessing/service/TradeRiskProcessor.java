package com.example.capitalmarkets.riskdataprocessing.service;

import com.example.capitalmarkets.riskdataprocessing.model.TradeInput;
import com.example.capitalmarkets.riskdataprocessing.model.TradeRiskResult;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TradeRiskProcessor implements ItemProcessor<TradeInput, TradeRiskResult> {

    private final LocalDate businessDate;
    public TradeRiskProcessor(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    @Override
    public TradeRiskResult process(TradeInput trade) throws Exception {
        if (trade.price()==null ||
                trade.price().compareTo(BigDecimal.ZERO)<0){
            throw new IllegalArgumentException("Price cannot be negative");
        }

        BigDecimal exposure = trade.quantity().multiply(trade.price());
        return new TradeRiskResult(
                trade.tradeId(), trade.instrument(), trade.currency(),
                trade.quantity(),trade.price(), exposure, businessDate
        );    }
}

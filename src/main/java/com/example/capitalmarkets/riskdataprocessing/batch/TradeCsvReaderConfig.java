package com.example.capitalmarkets.riskdataprocessing.batch;

import com.example.capitalmarkets.riskdataprocessing.model.TradeInput;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;

@Configuration
public class TradeCsvReaderConfig {

    @Bean
    public FlatFileItemReader<TradeInput> tradeItemReader(
            @Value("${risk.batch.input.path}") String inputPath
    ){
        FieldSetMapper<TradeInput> mapper = fieldSet ->
                new TradeInput(
                        fieldSet.readString("tradeId"),
                        fieldSet.readString("instrument"),
                        fieldSet.readString("currency"),
                        new BigDecimal(fieldSet.readString("quantity")),
                        new BigDecimal(fieldSet.readString("price"))
                );
        return new FlatFileItemReaderBuilder<TradeInput>()
                .name("tradeReader")
                //.resource(new ClassPathResource("input/trades.csv"))
                .resource(new ClassPathResource(inputPath))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("tradeId", "instrument", "currency", "quantity", "price")
                .fieldSetMapper(mapper)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<TradeInput> synchronizedTradeReader(
            FlatFileItemReader<TradeInput> tradeItemReader
    ){
        return new SynchronizedItemStreamReaderBuilder<TradeInput>()
                .delegate(tradeItemReader)
                .build();
    }
}

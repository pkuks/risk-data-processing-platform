package com.example.capitalmarkets.riskdataprocessing.batch;

import com.example.capitalmarkets.riskdataprocessing.model.TradeRiskResult;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Date;

@Configuration
public class TradeRiskWriterConfig {

    private final DataSource dataSource;

    public TradeRiskWriterConfig(DataSource dataSource){
        this.dataSource = dataSource;
    }

    @Bean
    public JdbcBatchItemWriter<TradeRiskResult> tradeRiskItemWriter(){
        return new JdbcBatchItemWriterBuilder<TradeRiskResult>()
                .dataSource(dataSource)
                .sql("INSERT INTO trade_risk_result(" +
                        "trade_id, instrument, currency, quantity, " +
                        "price, exposure, business_date) " +
                        " VALUES(:tradeId,:instrument,:currency,:quantity," +
                        ":price,:exposure,:businessDate)")
                .beanMapped()
                .assertUpdates(true)
                .build();
//        return new JdbcBatchItemWriterBuilder<TradeRiskResult>()
//                .dataSource(dataSource)
//                .sql("""
//                        INSERT INTO trade_risk_result(
//                            tradeId, instrument, currency, quantity,
//                            price, exposure, business_date)
//                        VALUES(?,?,?,?,?,?,?)
//                        """)
//                .itemPreparedStatementSetter(((item, ps) -> {
//                    ps.setString(1, item.tradeId());
//                    ps.setString(2, item.instrument());
//                    ps.setString(3, item.currency());
//                    ps.setBigDecimal(4, item.quantity());
//                    ps.setBigDecimal(5, item.price());
//                    ps.setBigDecimal(6, item.exposure());
//                    ps.setDate(7, Date.valueOf(item.businessDate()));
//                })).build();
    }
}

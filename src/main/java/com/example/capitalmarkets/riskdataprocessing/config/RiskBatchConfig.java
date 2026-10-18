package com.example.capitalmarkets.riskdataprocessing.config;

import com.example.capitalmarkets.riskdataprocessing.model.TradeInput;
import com.example.capitalmarkets.riskdataprocessing.model.TradeRiskResult;
import com.example.capitalmarkets.riskdataprocessing.service.TradeRiskProcessor;
import com.example.capitalmarkets.riskdataprocessing.service.TradeRiskSkipListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
public class RiskBatchConfig {

    @Bean
    public Step tradeRiskStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<TradeInput> tradeReader,
            TradeRiskProcessor tradeRiskProcessor,
            JdbcBatchItemWriter<TradeRiskResult> tradeRiskWriter,
            TradeRiskSkipListener skipListener){

        return new StepBuilder("tradeRiskStep", jobRepository)
                .<TradeInput, TradeRiskResult>chunk(1000, transactionManager)
                .reader(tradeReader)
                .processor(tradeRiskProcessor)
                .writer(tradeRiskWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skip(org.springframework.dao.DataIntegrityViolationException.class)
                .skipLimit(100)
                // Explicitly register the step lifecycle listener role
                .listener((StepExecutionListener) skipListener)
                // Explicitly register the item skip listener role
                .listener((SkipListener<TradeInput, TradeRiskResult>) skipListener)
                //.listener(skipListener)
                .build();
    }

    @Bean
    public Job tradeRiskJob(JobRepository jobRepository, Step tradeRiskstep){
        return new JobBuilder("tradeRiskJob", jobRepository)
                .start(tradeRiskstep)
                .build();
    }

    @Bean
    @StepScope
    public TradeRiskProcessor tradeRiskProcessor(
            @Value("#{jobParameters['businessDate']}") LocalDate businessDate){
        //@Value("#{jobParameters['businessDate']}") String businessDate) {
            return new TradeRiskProcessor(businessDate);
    }
}

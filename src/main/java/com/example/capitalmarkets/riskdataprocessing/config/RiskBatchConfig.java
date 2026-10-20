package com.example.capitalmarkets.riskdataprocessing.config;

import com.example.capitalmarkets.riskdataprocessing.model.TradeInput;
import com.example.capitalmarkets.riskdataprocessing.model.TradeRiskResult;
import com.example.capitalmarkets.riskdataprocessing.service.ReferenceDataClient;
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
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class RiskBatchConfig {

    @Bean
    public Step tradeRiskStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<TradeInput> synchronizedTradeReader,
            FlatFileItemReader<TradeInput> tradeReader,
            TradeRiskProcessor tradeRiskProcessor,
            JdbcBatchItemWriter<TradeRiskResult> tradeRiskWriter,
            TradeRiskSkipListener skipListener,
            TaskExecutor batchTaskExecutor){

        return new StepBuilder("tradeRiskStep", jobRepository)
                .<TradeInput, TradeRiskResult>chunk(1000, transactionManager)
                .reader(synchronizedTradeReader)
                .processor(tradeRiskProcessor)
                .writer(tradeRiskWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skip(org.springframework.dao.DataIntegrityViolationException.class)
                .skip(HttpClientErrorException.class) // ◄─ Fixes the 404 Not Found error
                .skip(RestClientException.class)       // ◄─ Safety net for other network/client errors
                .skipLimit(100)
                // Explicitly register the step lifecycle listener role
                .listener((StepExecutionListener) skipListener)
                // Explicitly register the item skip listener role
                .listener((SkipListener<TradeInput, TradeRiskResult>) skipListener)
                //.listener(skipListener)
                .stream(tradeReader)
                .taskExecutor(batchTaskExecutor)
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
            @Value("#{jobParameters['businessDate']}") LocalDate businessDate,
            ReferenceDataClient referenceDataClient){
        //@Value("#{jobParameters['businessDate']}") String businessDate) {
            return new TradeRiskProcessor(businessDate, referenceDataClient);
    }

    @Bean
    public TaskExecutor batchTaskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("RiskBatchThread-");
        executor.initialize();
        return executor;
    }

}

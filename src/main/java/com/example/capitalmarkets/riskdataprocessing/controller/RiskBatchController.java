package com.example.capitalmarkets.riskdataprocessing.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/risk/batch")
public class RiskBatchController {
    private final JobLauncher jobLauncher;
    private final Job tradeRiskJob;

    public RiskBatchController(JobLauncher jobLauncher,Job tradeRiskJob){
        this.jobLauncher = jobLauncher;
        this.tradeRiskJob = tradeRiskJob;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runBatch(
            @RequestParam
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
            LocalDate businessDate) throws Exception {

        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate("businessDate", businessDate)
                .toJobParameters();

        var execution = jobLauncher.run(tradeRiskJob, parameters);

        return ResponseEntity.accepted().body(Map.of(
                "jobExecutionId", String.valueOf(execution.getId()),
                "status", execution.getStatus().toString(),
                "businessDate", businessDate.toString()
        ));
    }
}

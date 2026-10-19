package com.example.capitalmarkets.riskdataprocessing.controller;

import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk/batch")
public class RiskBatchController {
    private final JobLauncher jobLauncher;
    private final Job tradeRiskJob;
    private final JobExplorer jobExplorer;

    public RiskBatchController(JobLauncher jobLauncher,Job tradeRiskJob,
                               JobExplorer jobExplorer){
        this.jobLauncher = jobLauncher;
        this.tradeRiskJob = tradeRiskJob;
        this.jobExplorer = jobExplorer;
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

    @GetMapping("/{executionId}/summary")
    public ResponseEntity<Map<String, Object>> getBatchSummary(
            @PathVariable Long executionId
    ){
        JobExecution execution = jobExplorer.getJobExecution(executionId);

        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        long readCount=0;
        long writeCount=0;
        long filterCount=0;
        long readSkipCount=0;
        long processSkipCount=0;
        long writeSkipCount=0;
        long rollbackCount=0;

        for (StepExecution step : execution.getStepExecutions()){
            readCount += step.getReadCount();
            writeCount += step.getWriteCount();
            filterCount += step.getFilterCount();
            readSkipCount += step.getReadSkipCount();
            processSkipCount += step.getProcessSkipCount();
            writeSkipCount += step.getWriteSkipCount();
            rollbackCount += step.getRollbackCount();
        }

        long totalSkipped = readSkipCount + processSkipCount + writeSkipCount;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("jobName", execution.getJobInstance().getJobName());
        summary.put("jobExecutionId", execution.getId());
        summary.put("jobInstanceId", execution.getJobInstance().getInstanceId());
        summary.put("businessDate", execution.getJobParameters().getLocalDate("businessDate"));
        summary.put("status", execution.getStatus().toString());
        summary.put("exitCode", execution.getExitStatus().getExitCode());
        summary.put("startTime", execution.getStartTime());
        summary.put("endTime", execution.getEndTime());

        summary.put("recordsRead", readCount);
        summary.put("recordsWritten", writeCount);
        summary.put("recordsFiltered", filterCount);

        summary.put("readSkipCount", readSkipCount);
        summary.put("processSkipCount", processSkipCount);
        summary.put("writeSkipCount", writeSkipCount);
        summary.put("totalSkipped", totalSkipped);

        summary.put("rollbackCount", rollbackCount);

        return ResponseEntity.ok(summary);

    }
}

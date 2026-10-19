package com.example.capitalmarkets.riskdataprocessing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest // ◄─ Injects JobLauncherTestUtils and StepScope Test Context
@SpringBootTest
@ActiveProfiles("test") // Use a separate application-test.properties (e.g., using an H2 database or clean schema)
class TradeRiskJobUnitTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate; // Used to assert database side effects

    @BeforeEach
    void cleanDatabase() {
        // Clear tables before each test execution to avoid bleeding state
        jdbcTemplate.update("DELETE FROM trade_risk_result");
        jdbcTemplate.update("DELETE FROM trade_risk_rejection");
    }

    @Test
    void should_SuccessfullyProcessBatch_When_ValidAndInvalidTradesExist() throws Exception {
        // 1. Arrange: Prepare Job Parameters mirroring your API Controller
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("businessDate", "2026-10-18") // ◄─ Your businessDate param
                .addLong("run.id", System.currentTimeMillis()) // Ensures uniqueness for restart/run validation
                .toJobParameters();

        // 2. Act: Launch the complete job via the testing utility
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 3. Assert Job Status
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 4. Assert Business Database Outcomes (JdbcBatchItemWriter)
        // Assuming your test 'trades.csv' sample data has 3 valid trades
        Integer validTradeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_risk_result", Integer.class);
        assertThat(validTradeCount).isEqualTo(4);

        // 5. Assert Regulatory Rejection Outcomes (SkipListener + REQUIRES_NEW)
        // Assuming your test data contains 1 malformed record that triggers a skip
        Integer rejectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_risk_rejection", Integer.class);
        assertThat(rejectionCount).isEqualTo(1);

        // Validate that your SkipListener successfully recorded the metadata context
        Map<String, Object> rejectedRecord = jdbcTemplate.queryForMap(
                "SELECT job_execution_id, rejection_reason FROM trade_risk_rejection LIMIT 1");

        assertThat(rejectedRecord.get("job_execution_id")).isEqualTo(jobExecution.getId());
        assertThat(rejectedRecord.get("rejection_reason").toString()).contains("negative");
    }
}

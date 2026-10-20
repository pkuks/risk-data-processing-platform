package com.example.capitalmarkets.riskdataprocessing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest // ◄─ Injects JobLauncherTestUtils and StepScope Test Context
@SpringBootTest
@ActiveProfiles("test") // Use a separate application-test.properties (e.g., using an H2 database or clean schema)
@AutoConfigureWireMock(port=8082)
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
    void should_SuccessfullyProcessBatch_With_ExternalReferenceData() throws Exception {
        // 1. Arrange: Stub out HTTP responses for instruments inside your 'trades-test.csv' file

        // Mock valid instrument 1
        stubFor(get(urlEqualTo("/api/reference-data/EQUITY"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "instrument": "EQUITY",
                                    "valuationPrice": 150.00,
                                    "currency": "USD"
                                }
                                """)));

        // Mock valid instrument 2
        stubFor(get(urlEqualTo("/api/reference-data/BOND"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "instrument": "BOND",
                                    "valuationPrice": 300.00,
                                    "currency": "USD"
                                }
                                """)));

        // Mock an invalid instrument that throws an error (to trigger your SkipListener)
        stubFor(get(urlEqualTo("/api/reference-data/UNKNOWN"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        // 2. Act: Prepare parameters and launch the batch job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("businessDate", "2026-10-20")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 3. Assert: Core Batch Execution Status
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 4. Assert: Database Outcomes (Valid Trades written via JdbcBatchItemWriter)
        Integer validTradeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_risk_result", Integer.class);
        assertThat(validTradeCount).isEqualTo(2); // Assumes 2 trades successfully fetched reference prices

        // 5. Assert: Audit Trail Outcomes (Invalid reference tickers handled by SkipListener)
        Integer rejectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_risk_rejection", Integer.class);
        assertThat(rejectionCount).isEqualTo(1); // 1 trade skipped due to HTTP error mapping

        Map<String, Object> rejectedRecord = jdbcTemplate.queryForMap(
                "SELECT job_execution_id, rejection_reason FROM trade_risk_rejection LIMIT 1");

        assertThat(rejectedRecord.get("job_execution_id")).isEqualTo(jobExecution.getId());
        assertThat(rejectedRecord.get("rejection_reason").toString()).contains("404");
    }

//    @Test
//    void should_SuccessfullyProcessBatch_When_ValidAndInvalidTradesExist() throws Exception {
//        // 1. Arrange: Prepare Job Parameters mirroring your API Controller
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("businessDate", "2026-10-18") // ◄─ Your businessDate param
//                .addLong("run.id", System.currentTimeMillis()) // Ensures uniqueness for restart/run validation
//                .toJobParameters();
//
//        // 2. Act: Launch the complete job via the testing utility
//        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
//
//        // 3. Assert Job Status
//        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
//        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
//
//        // 4. Assert Business Database Outcomes (JdbcBatchItemWriter)
//        // Assuming your test 'trades.csv' sample data has 3 valid trades
//        Integer validTradeCount = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM trade_risk_result", Integer.class);
//        assertThat(validTradeCount).isEqualTo(4);
//
//        // 5. Assert Regulatory Rejection Outcomes (SkipListener + REQUIRES_NEW)
//        // Assuming your test data contains 1 malformed record that triggers a skip
//        Integer rejectionCount = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM trade_risk_rejection", Integer.class);
//        assertThat(rejectionCount).isEqualTo(1);
//
//        // Validate that your SkipListener successfully recorded the metadata context
//        Map<String, Object> rejectedRecord = jdbcTemplate.queryForMap(
//                "SELECT job_execution_id, rejection_reason FROM trade_risk_rejection LIMIT 1");
//
//        assertThat(rejectedRecord.get("job_execution_id")).isEqualTo(jobExecution.getId());
//        assertThat(rejectedRecord.get("rejection_reason").toString()).contains("negative");
//    }
}

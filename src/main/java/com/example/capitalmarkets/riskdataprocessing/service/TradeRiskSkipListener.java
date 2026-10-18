package com.example.capitalmarkets.riskdataprocessing.service;

import com.example.capitalmarkets.riskdataprocessing.model.TradeInput;
import com.example.capitalmarkets.riskdataprocessing.model.TradeRiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TradeRiskSkipListener implements SkipListener<TradeInput, TradeRiskResult>, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(TradeRiskSkipListener.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private Long jobExecutionId;

    public TradeRiskSkipListener(NamedParameterJdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    // Explicit interface method from StepExecutionListener
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.jobExecutionId = stepExecution.getJobExecutionId();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null; // Leave step status unchanged
    }

    @Override
    public void onSkipInProcess(TradeInput item, Throwable t){
        saveToRejectionTable(item.tradeId(), t, item.toString());
    }

    @Override
    public void onSkipInWrite(TradeRiskResult item, Throwable t) {
        saveToRejectionTable(item.tradeId(), t, item.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveToRejectionTable(String tradeId, Throwable t, String rawRecord){
        String sql = "INSERT INTO trade_risk_rejection (job_execution_id, input_trade_id, rejection_reason, raw_record) " +
                "VALUES (:jobExecutionId, :tradeId, :rejectionReason, :rawRecord)";

        String errorMessage = t.getMessage();
        String croppedReason = (errorMessage != null && errorMessage.length() > 1000)
                ? errorMessage.substring(0, 1000)
                : errorMessage;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobExecutionId", this.jobExecutionId)
                .addValue("tradeId", tradeId)
                .addValue("rejectionReason", croppedReason)
                .addValue("rawRecord", rawRecord);

        try {
            jdbcTemplate.update(sql, params);
            log.info("Successfully logged invalid trade {} for Job Execution ID {} to rejection table.", tradeId, jobExecutionId);
        } catch (Exception e) {
            log.error("Critical failure writing skip log to database for Trade ID: {}. Error: {}",
                    tradeId, e.getMessage(), e);
        }
    }
}

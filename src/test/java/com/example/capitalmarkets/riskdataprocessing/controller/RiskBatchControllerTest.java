package com.example.capitalmarkets.riskdataprocessing.controller;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskBatchController.class)
public class RiskBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean
    private Job tradeRiskJob;

    @MockitoBean
    private JobExplorer jobExplorer;

    @Test
    void should_ReturnBadRequestAndCustomJson_When_BusinessDateIsMissing() throws Exception {
        mockMvc.perform(post("/api/risk/batch/run")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("ERR_MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Required parameter 'businessDate' is missing."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_ReturnBadRequestAndCustomJson_When_BusinessDateFormatIsMalformed() throws Exception {
        mockMvc.perform(post("/api/risk/batch/run")
                        .param("businessDate", "19-10-2026") // ◄─ Non-ISO format (Expected YYYY-MM-DD)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("ERR_INVALID_PARAMETER_FORMAT"))
                .andExpect(jsonPath("$.message").value("Parameter 'businessDate' has an invalid value. Expected format is ISO Date (YYYY-MM-DD)."));
    }
    @Test
    void should_ReturnNotFound_When_SummaryExecutionIdDoesNotExist() throws Exception {
        when(jobExplorer.getJobExecution(999L)).thenReturn(null);

        mockMvc.perform(get("/api/risk/batch/999/summary"))
                .andExpect(status().isNotFound());
    }
}

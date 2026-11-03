package com.sprint.monew.backup.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.backup.application.ArticleBackupJobConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleBackupDemoController.class)
class ArticleBackupDemoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JobOperator jobOperator;

    @MockitoBean(name = "articleBackupJob")
    Job articleBackupJob;

    @Test
    @DisplayName("날짜를 받아 백업 배치를 실행하고 실행 상태를 응답한다")
    void backupRunsJobWithDate() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        given(execution.getStatus()).willReturn(BatchStatus.COMPLETED);
        given(jobOperator.start(eq(articleBackupJob), any(JobParameters.class))).willReturn(execution);

        mockMvc.perform(post("/api/dev/articles/backup").param("date", "2026-10-28"))
                .andExpect(status().isOk())
                .andExpect(content().string("COMPLETED"));

        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobOperator).start(eq(articleBackupJob), parameters.capture());
        assertThat(parameters.getValue().getLocalDate(ArticleBackupJobConfig.TARGET_DATE))
                .isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(parameters.getValue().getLong("requestedAt")).isNotNull();
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 400으로 응답하고 배치를 실행하지 않는다")
    void invalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dev/articles/backup").param("date", "2026/10/28"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobOperator);
    }
}

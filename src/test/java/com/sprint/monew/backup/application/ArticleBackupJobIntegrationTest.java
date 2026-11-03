package com.sprint.monew.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.sprint.monew.backup.application.port.FileStorage;
import com.sprint.monew.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class ArticleBackupJobIntegrationTest {

    @Autowired
    JobOperator jobOperator;

    @Autowired
    Job articleBackupJob;

    @MockitoBean
    FileStorage fileStorage;

    @Test
    @DisplayName("백업 배치를 실행하면 그 날짜 키로 파일을 저장하고 완료된다")
    void jobStoresBackupFile() throws Exception {
        LocalDate date = LocalDate.of(2001, 1, 1);

        JobExecution execution = jobOperator.start(
                articleBackupJob,
                new JobParametersBuilder()
                        .addLocalDate(ArticleBackupJobConfig.TARGET_DATE, date)
                        .addLong("requestedAt", System.nanoTime())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(fileStorage).put(eq("articles/2001-01-01.json"), any());
    }
}

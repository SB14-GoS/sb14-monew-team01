package com.sprint.monew.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

@ExtendWith(MockitoExtension.class)
class ArticleBackupSchedulerTest {

    // KST 2026-10-29 01:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T16:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    JobOperator jobOperator;

    @Mock
    Job articleBackupJob;

    private ArticleBackupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ArticleBackupScheduler(jobOperator, articleBackupJob, CLOCK);
    }

    @Test
    @DisplayName("KST 기준 어제 날짜를 파라미터로 백업 배치를 시작한다")
    void startsJobWithYesterday() throws Exception {
        scheduler.backupYesterday();

        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobOperator).start(eq(articleBackupJob), parameters.capture());
        assertThat(parameters.getValue().getLocalDate(ArticleBackupJobConfig.TARGET_DATE))
                .isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    @DisplayName("배치 시작에 실패해도 예외를 밖으로 던지지 않는다")
    void swallowsStartFailure() throws Exception {
        doThrow(new IllegalStateException("failure")).when(jobOperator).start(eq(articleBackupJob), any());

        assertThatCode(() -> scheduler.backupYesterday()).doesNotThrowAnyException();
    }
}

package com.sprint.monew.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {
    @Mock
    JobOperator jobOperator;

    @Mock
    Job cleanupJob;

    @Test
    @DisplayName("매일 한국 시간 3시에 실행하도록 설정되어 있다")
    void scheduledDailyInSeoul() throws Exception {
        Scheduled scheduled = NotificationCleanupScheduler.class
                .getMethod("cleanupNotifications")
                .getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("0 0 3 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("스케줄러는 실행 시각을 파라미터로 정리 배치를 시작한다")
    void startsCleanupJob() throws Exception {
        var scheduler = new NotificationCleanupScheduler(jobOperator, cleanupJob);
        Instant before = Instant.now();
        scheduler.cleanupNotifications();
        Instant after = Instant.now();

        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobOperator).start(eq(cleanupJob), parameters.capture());
        Instant requestedAt = Instant.parse(parameters.getValue().getString("requestedAt"));
        assertThat(requestedAt).isBetween(before, after);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("배치 시작 실패 시 요청 시각과 예외를 로그에 기록하고 예외를 전파하지 않는다")
    void logsJobStartFailure(CapturedOutput output) throws Exception {
        var cause = new IllegalStateException("failure");
        doThrow(cause).when(jobOperator).start(eq(cleanupJob), any(JobParameters.class));
        assertThatCode(() -> new NotificationCleanupScheduler(jobOperator, cleanupJob).cleanupNotifications())
                .doesNotThrowAnyException();
        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobOperator).start(eq(cleanupJob), parameters.capture());
        String requestedAt = parameters.getValue().getString("requestedAt");
        assertThat(output)
                .contains("알림 정리 배치 실행 실패: requestedAt=" + requestedAt, "java.lang.IllegalStateException: failure");
    }
}

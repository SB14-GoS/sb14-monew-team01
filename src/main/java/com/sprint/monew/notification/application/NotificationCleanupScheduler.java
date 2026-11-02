package com.sprint.monew.notification.application;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationCleanupScheduler {

    private final JobOperator jobOperator;
    private final Job notificationCleanupJob;

    // 매일 한국 시간 오전 3시라는 예시
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupNotifications() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("requestedAt", Instant.now().toString())
                .toJobParameters();

        try {
            jobOperator.start(notificationCleanupJob, parameters);
        } catch (Exception exception) {
            log.error("알림 정리 배치 실행 실패: requestedAt={}", parameters.getString("requestedAt"), exception);
        }
    }

    public NotificationCleanupScheduler(
            JobOperator jobOperator, @Qualifier("notificationCleanupJob") Job notificationCleanupJob) {
        this.jobOperator = jobOperator;
        this.notificationCleanupJob = notificationCleanupJob;
    }
}

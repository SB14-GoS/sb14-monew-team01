package com.sprint.monew.notification.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJobConfig {

    private final NotificationService notificationService;

    @Bean
    Step notificationCleanupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("notificationCleanupStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            String requestedAt = chunkContext
                                    .getStepContext()
                                    .getJobParameters()
                                    .get("requestedAt")
                                    .toString();
                            int deletedCount = notificationService.deleteExpireNotification(Instant.parse(requestedAt));

                            // 이번 Job 실행에 삭제 건수를 기록
                            chunkContext
                                    .getStepContext()
                                    .getStepExecution()
                                    .getJobExecution()
                                    .getExecutionContext()
                                    .putInt("deletedCount", deletedCount);

                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }

    @Bean
    Job notificationCleanupJob(JobRepository jobRepository, @Qualifier("notificationCleanupStep") Step step) {
        return new JobBuilder("notificationCleanupJob", jobRepository)
                // 배치실행 시작
                .start(step)
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        int deletedCount = jobExecution.getExecutionContext().getInt("deletedCount", 0);
                        log.info("알림 정리 배치 삭제 건수: {}", deletedCount);
                    }
                })
                .build();
    }
}

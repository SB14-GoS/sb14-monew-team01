package com.sprint.monew.notification.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
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
public class NotificationCleanupJobConfig {

    private final NotificationService notificationService;

    @Bean
    Step notificationCleanupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("notificationCleanupStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            notificationService.deleteExpireNotification(Instant.now());
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }

    @Bean
    Job notificationCleanupJob(JobRepository jobRepository, @Qualifier("notificationCleanupStep") Step step) {
        return new JobBuilder("notificationCleanupJob", jobRepository)
                .start(step)
                .build();
    }
}

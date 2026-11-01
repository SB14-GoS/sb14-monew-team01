package com.sprint.monew.user.application;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class UserPurgeJobConfig {
    public static final String JOB_NAME = "userPurgeJob";
    public static final String REQUESTED_AT = "requestedAt";
    private static final Duration RETENTION = Duration.ofDays(1);

    private final UserService userService;

    @Bean
    Job userPurgeJob(JobRepository jobRepository, Step userPurgeStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(userPurgeStep).build();
    }

    @Bean
    Step userPurgeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("userPurgeStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            Long requestedAt = chunkContext
                                    .getStepContext()
                                    .getStepExecution()
                                    .getJobParameters()
                                    .getLong(REQUESTED_AT);
                            Instant threshold =
                                    Instant.ofEpochMilli(requestedAt).minus(RETENTION);
                            int count = userService.purgeDeletedUsers(threshold);
                            // 삭제 건수 기록
                            contribution.incrementWriteCount(count);
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}

package com.sprint.monew.backup.application;

import java.time.LocalDate;
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
public class ArticleBackupJobConfig {

    public static final String JOB_NAME = "articleBackupJob";
    public static final String TARGET_DATE = "targetDate";

    private final ArticleBackupService articleBackupService;

    @Bean
    Job articleBackupJob(JobRepository jobRepository, Step articleBackupStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(articleBackupStep).build();
    }

    @Bean
    Step articleBackupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("articleBackupStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            // 백업 날짜 불러오기
                            LocalDate targetDate = chunkContext
                                    .getStepContext()
                                    .getStepExecution()
                                    .getJobParameters()
                                    .getLocalDate(TARGET_DATE);
                            int count = articleBackupService.backup(targetDate);
                            // 쓰기 건수 기록
                            contribution.incrementWriteCount(count);
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}

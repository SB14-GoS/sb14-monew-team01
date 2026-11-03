package com.sprint.monew.article.application;

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
public class ArticleCollectJobConfig {

    private final ArticleCollectService articleCollectService;

    @Bean
    Job articleCollectJob(JobRepository jobRepository, Step articleCollectStep) {
        return new JobBuilder("articleCollectJob", jobRepository)
                .start(articleCollectStep)
                .build();
    }

    @Bean
    Step articleCollectStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("articleCollectStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            articleCollectService.save();
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}

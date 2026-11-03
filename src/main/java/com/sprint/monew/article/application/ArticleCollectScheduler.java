package com.sprint.monew.article.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCollectScheduler {

    private final JobOperator jobOperator;
    private final Job articleCollectJob;

    @Scheduled(cron = "${monew.article-collect.cron:0 0 * * * *}", zone = "Asia/Seoul")
    public void collectArticles() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("requestedAt", Instant.now().toString())
                .toJobParameters();

        try {
            jobOperator.start(articleCollectJob, parameters);
        } catch (Exception exception) {
            log.error("기사 수집 배치 시작 실패: parameters={}", parameters, exception);
        }
    }
}

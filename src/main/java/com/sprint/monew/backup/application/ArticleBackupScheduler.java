package com.sprint.monew.backup.application;

import java.time.Clock;
import java.time.LocalDate;
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
public class ArticleBackupScheduler {

    private final JobOperator jobOperator;
    private final Job job;
    private final Clock clock;

    @Scheduled(cron = "${monew.backup.cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void backupYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate(ArticleBackupJobConfig.TARGET_DATE, yesterday)
                .toJobParameters();

        try {
            jobOperator.start(job, parameters);
        } catch (Exception exception) {
            log.error("기사 백업 배치 실행 실패: date={}", yesterday, exception);
        }
    }
}

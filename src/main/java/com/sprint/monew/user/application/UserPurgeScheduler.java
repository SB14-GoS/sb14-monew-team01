package com.sprint.monew.user.application;

import java.time.Clock;
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
public class UserPurgeScheduler {
    private final JobOperator jobOperator;
    private final Job userPurgeJob;
    private final Clock clock;

    @Scheduled(cron = "${monew.user-purge.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void purgeDeletedUsers() {
        Instant now = Instant.now(clock);
        JobParameters parameters = new JobParametersBuilder()
                .addLong(UserPurgeJobConfig.REQUESTED_AT, now.toEpochMilli())
                .toJobParameters();

        try {
            jobOperator.start(userPurgeJob, parameters);
        } catch (Exception exception) {
            log.error("사용자 물리 삭제 배치 실행 실패: requestedAt={}", now, exception);
        }
    }
}

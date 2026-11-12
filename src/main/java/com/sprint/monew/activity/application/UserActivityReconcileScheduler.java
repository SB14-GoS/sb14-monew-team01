package com.sprint.monew.activity.application;

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
public class UserActivityReconcileScheduler {

    private final JobOperator jobOperator;

    private final Job userActivityReconcileJob; // 빈 이름으로 주입 (Job 빈이 여러 개)

    private final Clock clock;

    @Scheduled(cron = "${monew.activity.reconcile.cron:0 0 5 * * *}", zone = "Asia/Seoul")
    public void reconcileDaily() {
        LocalDate today = LocalDate.now(clock);
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate(UserActivityReconcileJobConfig.TARGET_DATE, today)
                .toJobParameters();
        try {
            jobOperator.start(userActivityReconcileJob, parameters);
        } catch (Exception exception) {
            log.error("활동 내역 정합성 보정 배치 실행 실패: date={}", today, exception);
        }
    }
}

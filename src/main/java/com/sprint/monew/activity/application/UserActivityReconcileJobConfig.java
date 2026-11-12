package com.sprint.monew.activity.application;

import com.sprint.monew.activity.application.UserActivityReconciler.ReconcileResult;
import com.sprint.monew.global.metric.BatchMetricsListener;
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

/**
 * 활동 내역 정합성 보정 배치. outbox로도 메우지 못한 차이(DEAD 이벤트, Outbox 도입 전 유실, 수동 데이터 수정)를 원본 기준으로 맞춘다.
 *
 * <p>날짜를 파라미터로 받아 같은 날 한 번만 실행된다 (ECS 태스크가 여러 개여도 중복 실행 없음).
 */
@Configuration
@RequiredArgsConstructor
public class UserActivityReconcileJobConfig {

    public static final String JOB_NAME = "userActivityReconcileJob";
    public static final String TARGET_DATE = "targetDate";

    private final UserActivityReconciler reconciler;
    private final BatchMetricsListener batchMetricsListener;

    @Bean
    Job userActivityReconcileJob(JobRepository jobRepository, Step userActivityReconcileStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(userActivityReconcileStep)
                .listener(batchMetricsListener)
                .build();
    }

    @Bean
    Step userActivityReconcileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("userActivityReconcileStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            ReconcileResult result = reconciler.reconcileAll();
                            contribution.incrementWriteCount(result.repaired() + result.orphansRemoved());
                            chunkContext
                                    .getStepContext()
                                    .getStepExecution()
                                    .getExecutionContext()
                                    .putString("result", result.toString());
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .listener(batchMetricsListener)
                .build();
    }
}

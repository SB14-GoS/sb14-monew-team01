package com.sprint.monew.global.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;

class BatchMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private BatchMetrics batchMetrics;
    private BatchMetricsListener batchMetricsListener;

    @BeforeEach
    void setUp() {
        // 무거운 스프링 서버를 띄우지 않고 메모리에서 빠르게 테스트할 수 있는 간이 계기판
        meterRegistry = new SimpleMeterRegistry();
        batchMetrics = new BatchMetrics(meterRegistry);
        batchMetricsListener = new BatchMetricsListener(batchMetrics);
    }

    @Test
    @DisplayName("수집 건수, 실패 건수, 소요 시간 메트릭이 정상적으로 기록된다")
    void recordMetricsSuccess() {
        // when: 정상 값 기록
        batchMetrics.incrementCollected("articleCollectJob", 15);
        batchMetrics.incrementFailed("articleCollectJob", 2);
        batchMetrics.recordDuration("articleCollectJob", "COMPLETED", Duration.ofSeconds(3));

        // when: 0 이하 또는 비정상 값은 무시되는지 검증
        batchMetrics.incrementCollected("articleCollectJob", 0);
        batchMetrics.incrementFailed("articleCollectJob", -1);
        batchMetrics.recordDuration("articleCollectJob", "COMPLETED", null);
        batchMetrics.recordDuration("articleCollectJob", "COMPLETED", Duration.ofSeconds(-1));

        // then: 계기판에서 값을 꺼내어 정확히 기록되었는지 확인
        Counter collectedCounter = meterRegistry
                .find(BatchMetrics.COLLECTED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();
        Counter failedCounter = meterRegistry
                .find(BatchMetrics.FAILED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();
        Timer durationTimer = meterRegistry
                .find(BatchMetrics.DURATION_METRIC)
                .tag("job", "articleCollectJob")
                .tag("status", "COMPLETED")
                .timer();

        assertThat(collectedCounter).isNotNull();
        assertThat(collectedCounter.count()).isEqualTo(15.0);

        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(2.0);

        assertThat(durationTimer).isNotNull();
        assertThat(durationTimer.count()).isEqualTo(1L);
        assertThat(durationTimer.totalTime(TimeUnit.SECONDS)).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Job 종료 시 리스너를 통해 소요 시간과 실패 여부가 기록된다")
    void afterJobRecordsDurationAndFailure() {
        // given: 5초 동안 돌다가 실패(FAILED)한 가짜 Job 상황 연출
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");
        LocalDateTime start = LocalDateTime.now().minusSeconds(5);
        LocalDateTime end = LocalDateTime.now();

        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(jobExecution.getStatus()).willReturn(BatchStatus.FAILED);
        given(jobExecution.getStartTime()).willReturn(start);
        given(jobExecution.getEndTime()).willReturn(end);

        // when: 리스너의 afterJob 실행
        batchMetricsListener.afterJob(jobExecution);

        // then: 소요 시간 타이머가 1회 작동했고, 실패 카운터가 1 올라갔는지 확인
        Timer timer = meterRegistry
                .find(BatchMetrics.DURATION_METRIC)
                .tag("job", "articleCollectJob")
                .tag("status", "FAILED")
                .timer();
        Counter failedCounter = meterRegistry
                .find(BatchMetrics.FAILED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Step 종료 시 리스너를 통해 수집 건수와 실패 건수가 기록된다")
    void afterStepRecordsCollectedAndFailedCounts() {
        // given: 20건 저장 성공하고, 총 4건(스킵 3 + 예외 1) 실패한 가짜 Step 상황 연출
        StepExecution stepExecution = mock(StepExecution.class);
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");

        given(stepExecution.getJobExecution()).willReturn(jobExecution);
        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(stepExecution.getWriteCount()).willReturn(20L);
        given(stepExecution.getReadSkipCount()).willReturn(1L);
        given(stepExecution.getProcessSkipCount()).willReturn(1L);
        given(stepExecution.getWriteSkipCount()).willReturn(1L);
        given(stepExecution.getFailureExceptions()).willReturn(List.of(new RuntimeException("error")));
        given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

        // when: 리스너의 afterStep 실행
        ExitStatus exitStatus = batchMetricsListener.afterStep(stepExecution);

        // then: 수집 20건, 실패 4건이 정확히 기록되었는지 확인
        Counter collectedCounter = meterRegistry
                .find(BatchMetrics.COLLECTED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();
        Counter failedCounter = meterRegistry
                .find(BatchMetrics.FAILED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();

        assertThat(exitStatus).isEqualTo(ExitStatus.COMPLETED);
        assertThat(collectedCounter).isNotNull();
        assertThat(collectedCounter.count()).isEqualTo(20.0);
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(4.0);
    }
}

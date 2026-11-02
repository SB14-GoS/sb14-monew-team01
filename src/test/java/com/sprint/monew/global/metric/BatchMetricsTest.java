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
    @DisplayName("Job이 FAILED 상태로 종료되면 고정된 시작·종료 시각 기준 소요 시간과 실패 건수가 기록된다")
    void afterJobRecordsDurationAndFailure() {
        // given: 정확히 5초 동안 실행되다 실패(FAILED)한 Job 상황 연출
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");
        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 2, 0, 0, 5);

        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(jobExecution.getStatus()).willReturn(BatchStatus.FAILED);
        given(jobExecution.getStartTime()).willReturn(start);
        given(jobExecution.getEndTime()).willReturn(end);

        // when
        batchMetricsListener.afterJob(jobExecution);

        // then: 타이머 작동 횟수(1회), 총 소요 시간(5초), 실패 카운터(1건) 검증
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
        assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(5.0);
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Job이 COMPLETED 상태로 종료되면 소요 시간만 기록되고 실패 건수는 증가하지 않는다")
    void afterJobCompletedRecordsOnlyDuration() {
        // given: 정확히 4초 동안 실행되고 성공(COMPLETED)한 Job 상황 연출
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");
        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 2, 0, 0, 4);

        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(jobExecution.getStatus()).willReturn(BatchStatus.COMPLETED);
        given(jobExecution.getStartTime()).willReturn(start);
        given(jobExecution.getEndTime()).willReturn(end);

        // when
        batchMetricsListener.afterJob(jobExecution);

        // then: COMPLETED 타이머는 4초로 기록되고, 실패 카운터는 생성·증가하지 않음
        Timer timer = meterRegistry
                .find(BatchMetrics.DURATION_METRIC)
                .tag("job", "articleCollectJob")
                .tag("status", "COMPLETED")
                .timer();
        Counter failedCounter = meterRegistry
                .find(BatchMetrics.FAILED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(4.0);
        assertThat(failedCounter).isNull();
    }

    @Test
    @DisplayName("Job 종료 시각이 없으면 소요 시간 타이머는 기록하지 않고 실패 건수만 기록한다")
    void afterJobWithoutEndTimeRecordsOnlyFailure() {
        // given: 종료 시각(endTime)이 null인 상태에서 FAILED된 경계값 연출
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");
        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 0, 0, 0);

        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(jobExecution.getStatus()).willReturn(BatchStatus.FAILED);
        given(jobExecution.getStartTime()).willReturn(start);
        given(jobExecution.getEndTime()).willReturn(null);

        // when
        batchMetricsListener.afterJob(jobExecution);

        // then: 타이머는 기록되지 않고(null), 실패 건수만 1 증가함
        Timer timer = meterRegistry
                .find(BatchMetrics.DURATION_METRIC)
                .tag("job", "articleCollectJob")
                .tag("status", "FAILED")
                .timer();
        Counter failedCounter = meterRegistry
                .find(BatchMetrics.FAILED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();

        assertThat(timer).isNull();
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Step 완료 시 리스너를 통해 수집 건수와 스킵 기반 실패 건수가 기록된다")
    void afterStepRecordsCollectedAndFailedCounts() {
        // given: 20건 저장 성공하고, 스킵 3건(읽기 1 + 처리 1 + 쓰기 1)이 발생한 완료(COMPLETED) Step 연출
        StepExecution stepExecution = mock(StepExecution.class);
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");

        given(stepExecution.getJobExecution()).willReturn(jobExecution);
        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(stepExecution.getWriteCount()).willReturn(20L);
        given(stepExecution.getReadSkipCount()).willReturn(1L);
        given(stepExecution.getProcessSkipCount()).willReturn(1L);
        given(stepExecution.getWriteSkipCount()).willReturn(1L);
        given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

        // when: 리스너의 afterStep 실행
        ExitStatus exitStatus = batchMetricsListener.afterStep(stepExecution);

        // then: 수집 20건과 스킵 3건이 기록되고 COMPLETED 상태를 반환하는지 확인
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
        assertThat(failedCounter.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Step 예외는 실패 건수에 포함되지 않는다")
    void afterStepExcludesFailureExceptionFromFailedCount() {
        // given: 스킵 3건과 Step 실행 예외 1건이 함께 발생해 FAILED된 Step 연출
        StepExecution stepExecution = mock(StepExecution.class);
        JobExecution jobExecution = mock(JobExecution.class);
        JobInstance jobInstance = new JobInstance(1L, "articleCollectJob");

        given(stepExecution.getJobExecution()).willReturn(jobExecution);
        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(stepExecution.getStatus()).willReturn(BatchStatus.FAILED);
        given(stepExecution.getReadSkipCount()).willReturn(1L);
        given(stepExecution.getProcessSkipCount()).willReturn(1L);
        given(stepExecution.getWriteSkipCount()).willReturn(1L);
        given(stepExecution.getExitStatus()).willReturn(ExitStatus.FAILED);
        given(stepExecution.getFailureExceptions()).willReturn(List.of(new RuntimeException("step failure")));

        // when
        ExitStatus exitStatus = batchMetricsListener.afterStep(stepExecution);

        // then: Step 예외(1건)는 제외되고 스킵 건수(3건)만 실패 건수에 반영됨
        Counter failedCounter = meterRegistry
                .find(BatchMetrics.FAILED_COUNT_METRIC)
                .tag("job", "articleCollectJob")
                .counter();

        assertThat(exitStatus).isEqualTo(ExitStatus.FAILED);
        assertThat(failedCounter).isNotNull();
        assertThat(failedCounter.count()).isEqualTo(3.0);
    }
}

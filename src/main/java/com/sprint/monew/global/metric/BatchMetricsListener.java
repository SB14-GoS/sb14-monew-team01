package com.sprint.monew.global.metric;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchMetricsListener implements JobExecutionListener, StepExecutionListener {

    private final BatchMetrics batchMetrics;

    // 전체 배치 작업이 끝났을 때 자동으로 실행되는 메서드
    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().name();

        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        // 소요 시간 기록
        if (startTime != null && endTime != null) {
            batchMetrics.recordDuration(jobName, status, Duration.between(startTime, endTime));
        }

        // 실패 건수 +1 기록
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            batchMetrics.incrementFailed(jobName, 1);
        }
    }

    // 배치 내부의 세부 단계(Step)가 끝났을 때 자동으로 실행되는 메서드
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();

        long collectedCount = stepExecution.getWriteCount();

        // 읽다가 스킵된 건수 + 가공하다 스킵된 건수 + 쓰다가 스킵된 건수 + 발생한 에러 개수 합산
        long failedCount = stepExecution.getReadSkipCount()
                + stepExecution.getProcessSkipCount()
                + stepExecution.getWriteSkipCount()
                + stepExecution.getFailureExceptions().size();

        batchMetrics.incrementCollected(jobName, collectedCount);
        batchMetrics.incrementFailed(jobName, failedCount);

        return stepExecution.getExitStatus();
    }
}

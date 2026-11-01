package com.sprint.monew.global.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class BatchMetrics {
    public static final String COLLECTED_COUNT_METRIC = "monew.batch.collected.count";
    public static final String FAILED_COUNT_METRIC = "monew.batch.failed.count";
    public static final String DURATION_METRIC = "monew.batch.duration";

    private static final String DEFAULT_TAG_VALUE = "default";

    private final MeterRegistry meterRegistry;

    public BatchMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initMetrics();
    }

    private void initMetrics() {
        Counter.builder(COLLECTED_COUNT_METRIC)
                .description("배치 작업으로 수집된 데이터 건수")
                .tag("job", DEFAULT_TAG_VALUE)
                .register(meterRegistry);

        Counter.builder(FAILED_COUNT_METRIC)
                .description("배치 작업 중 실패한 데이터 건수")
                .tag("job", DEFAULT_TAG_VALUE)
                .register(meterRegistry);

        Timer.builder(DURATION_METRIC)
                .description("배치 작업 실행 소요 시간")
                .tag("job", DEFAULT_TAG_VALUE)
                .tag("status", "INITIALIZED")
                .register(meterRegistry);
    }

    // 수집 건수 증가 기록
    public void incrementCollected(String jobName, double count) {
        if (count <= 0) {
            return;
        }
        Counter.builder(COLLECTED_COUNT_METRIC)
                .description("배치 작업으로 수집된 데이터 건수")
                .tag("job", jobName)
                .register(meterRegistry)
                .increment(count);
    }

    // 실패 건수 증가 기록
    public void incrementFailed(String jobName, double count) {
        if (count <= 0) {
            return;
        }
        Counter.builder(FAILED_COUNT_METRIC)
                .description("배치 작업 중 실패한 데이터 건수")
                .tag("job", jobName)
                .register(meterRegistry)
                .increment(count);
    }

    // 배치 실행 소요 시간 기록
    public void recordDuration(String jobName, String status, Duration duration) {
        if (duration == null || duration.isNegative()) {
            return;
        }
        Timer.builder(DURATION_METRIC)
                .description("배치 작업 실행 소요 시간")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .record(duration);
    }
}

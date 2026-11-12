package com.sprint.monew.activity.perf;

import java.util.Arrays;
import java.util.function.IntConsumer;

/** 성능 측정용 지연 시간 통계. 워밍업 호출은 통계에서 제외한다. */
record LatencyStats(int samples, double avgMs, double p50Ms, double p95Ms, double maxMs) {

    /**
     * task를 warmUp + iterations 번 호출하고 뒤쪽 iterations 번의 시간만 잰다.
     *
     * @param task 호출 순번(0부터)을 받는다. 매 호출마다 다른 대상을 쓰도록 순번을 넘긴다.
     */
    static LatencyStats measure(int warmUp, int iterations, IntConsumer task) {
        for (int i = 0; i < warmUp; i++) {
            task.accept(i);
        }
        long[] elapsed = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            task.accept(warmUp + i);
            elapsed[i] = System.nanoTime() - start;
        }
        return of(elapsed);
    }

    static LatencyStats of(long[] nanos) {
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        double avg = Arrays.stream(sorted).average().orElse(0);
        return new LatencyStats(
                sorted.length,
                toMs(avg),
                toMs(percentile(sorted, 0.50)),
                toMs(percentile(sorted, 0.95)),
                toMs(sorted[sorted.length - 1]));
    }

    String toRow(String label) {
        return "| %s | %d | %.3f | %.3f | %.3f | %.3f |".formatted(label, samples, avgMs, p50Ms, p95Ms, maxMs);
    }

    static String header() {
        return "| 항목 | 표본 | 평균(ms) | p50(ms) | p95(ms) | 최대(ms) |\n| --- | --- | --- | --- | --- | --- |";
    }

    private static long percentile(long[] sorted, double ratio) {
        int index = (int) Math.ceil(sorted.length * ratio) - 1;
        return sorted[Math.max(0, index)];
    }

    private static double toMs(double nanos) {
        return nanos / 1_000_000.0;
    }
}

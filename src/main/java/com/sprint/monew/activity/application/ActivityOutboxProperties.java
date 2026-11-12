package com.sprint.monew.activity.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 활동 내역 outbox relay 설정 ({@code monew.activity.outbox.*}).
 *
 * @param relayEnabled 스케줄러로 relay를 돌릴지 여부 (테스트에서는 끄고 직접 호출한다)
 * @param pollDelayMs relay 실행 간격
 * @param batchSize 한 번에 읽을 이벤트 수
 * @param maxAttempts 데이터 문제로 실패한 이벤트를 DEAD로 보내기 전까지 시도 횟수 (인프라 장애는 제한 없이 재시도)
 * @param baseBackoff 첫 재시도 대기 시간, 실패할 때마다 두 배
 * @param maxBackoff 재시도 대기 시간 상한
 * @param retention 반영 완료(DONE) 이벤트 보관 기간
 */
@ConfigurationProperties("monew.activity.outbox")
public record ActivityOutboxProperties(
        @DefaultValue("true") boolean relayEnabled,
        @DefaultValue("1000") long pollDelayMs,
        @DefaultValue("100") int batchSize,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("1s") Duration baseBackoff,
        @DefaultValue("1m") Duration maxBackoff,
        @DefaultValue("7d") Duration retention) {

    Duration backoff(int attempts) {
        int exponent = Math.min(Math.max(attempts - 1, 0), 20);
        Duration delay = baseBackoff.multipliedBy(1L << exponent);
        return delay.compareTo(maxBackoff) > 0 ? maxBackoff : delay;
    }
}

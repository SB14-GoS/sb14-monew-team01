package com.sprint.monew.activity.application;

import com.sprint.monew.activity.domain.ActivityOutbox;
import com.sprint.monew.activity.domain.ActivityOutboxRepository;
import com.sprint.monew.activity.domain.ActivityOutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * outbox의 PENDING 이벤트를 id 순서대로 MongoDB 활동 내역에 반영한다.
 *
 * <ul>
 *   <li>advisory lock을 잡은 인스턴스 하나만 실행한다. 여러 인스턴스가 나눠 처리하면 같은 사용자 이벤트의 순서가 뒤바뀔 수 있다.
 *   <li>실패하면 그 이벤트에서 멈춘다. 뒤 이벤트를 먼저 반영하면 순서가 깨지기 때문이다 (예: 댓글 삭제가 작성보다 먼저 반영).
 *   <li>MongoDB 연결 실패 같은 인프라 장애는 횟수 제한 없이 지수 백오프로 재시도한다. 장애가 길어도 이벤트를 버리지 않는다.
 *   <li>역직렬화 실패 같은 데이터 문제는 {@code maxAttempts}번 실패하면 DEAD로 보내고 다음 이벤트로 넘어간다.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityOutboxRelay {

    static final long LOCK_KEY = 0x6163746976697479L; // "activity"

    private final ActivityOutboxRepository outboxRepository;
    private final ActivityEventCodec codec;
    private final UserActivityProjector projector;
    private final ActivityOutboxProperties properties;
    private final Clock clock;

    /** 한 배치를 반영한다. 다음 배치가 더 있을 수 있으면 {@link RelayResult#hasMore()}가 true다. */
    @Transactional
    public RelayResult relayBatch() {
        if (!outboxRepository.tryTransactionLock(LOCK_KEY)) {
            return RelayResult.locked();
        }
        List<ActivityOutbox> batch = outboxRepository.findByStatusOrderByIdAsc(
                ActivityOutboxStatus.PENDING, Limit.of(properties.batchSize()));

        int done = 0;
        int dead = 0;
        for (ActivityOutbox outbox : batch) {
            Instant now = clock.instant();
            if (!outbox.isDue(now)) {
                return new RelayResult(done, dead, false, true);
            }
            try {
                projector.project(codec.deserialize(outbox.getEventType(), outbox.getPayload()), outbox.getId());
                outbox.markDone(now);
                done++;
            } catch (RuntimeException exception) {
                if (isInfrastructureFailure(exception)) {
                    retry(outbox, exception, now);
                    return new RelayResult(done, dead, false, true);
                }
                if (outbox.getAttempts() + 1 >= properties.maxAttempts()) {
                    outbox.markDead(describe(exception), now);
                    dead++;
                    log.error(
                            "활동 내역 이벤트 반영 포기 - outboxId: {}, type: {}",
                            outbox.getId(),
                            outbox.getEventType(),
                            exception);
                    continue;
                }
                retry(outbox, exception, now);
                return new RelayResult(done, dead, false, true);
            }
        }
        return new RelayResult(done, dead, batch.size() == properties.batchSize(), false);
    }

    @Transactional
    public int purgeProcessed() {
        Instant before = clock.instant().minus(properties.retention());
        return outboxRepository.deleteProcessedBefore(ActivityOutboxStatus.DONE, before);
    }

    private void retry(ActivityOutbox outbox, RuntimeException exception, Instant now) {
        Instant next = now.plus(properties.backoff(outbox.getAttempts() + 1));
        outbox.markRetry(describe(exception), next);
        log.warn(
                "활동 내역 이벤트 반영 실패, 재시도 예정 - outboxId: {}, type: {}, attempts: {}, next: {}, cause: {}",
                outbox.getId(),
                outbox.getEventType(),
                outbox.getAttempts(),
                next,
                exception.toString());
    }

    private static boolean isInfrastructureFailure(RuntimeException exception) {
        return exception instanceof DataAccessResourceFailureException
                || exception instanceof TransientDataAccessException
                || exception instanceof RecoverableDataAccessException;
    }

    private static String describe(RuntimeException exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }

    /**
     * @param done 반영한 이벤트 수
     * @param dead 포기한 이벤트 수
     * @param hasMore 배치가 가득 차서 바로 다음 배치를 돌려도 되는지
     * @param blocked 재시도 대기 중이거나 실패해서 멈췄는지
     */
    public record RelayResult(int done, int dead, boolean hasMore, boolean blocked) {

        static RelayResult locked() {
            return new RelayResult(0, 0, false, true);
        }
    }
}

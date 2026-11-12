package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.activity.domain.ActivityOutbox;
import com.sprint.monew.activity.domain.ActivityOutboxRepository;
import com.sprint.monew.activity.domain.ActivityOutboxStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class ActivityOutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");

    @Mock
    ActivityOutboxRepository outboxRepository;

    @Mock
    ActivityEventCodec codec;

    @Mock
    UserActivityProjector projector;

    private final ActivityOutboxProperties properties = new ActivityOutboxProperties(
            true, 1000, 3, 3, Duration.ofSeconds(1), Duration.ofSeconds(10), Duration.ofDays(7));

    private ActivityOutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new ActivityOutboxRelay(
                outboxRepository, codec, projector, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("lock을 잡지 못하면 아무것도 읽지 않는다")
    void skipsWhenLocked() {
        given(outboxRepository.tryTransactionLock(anyLong())).willReturn(false);

        ActivityOutboxRelay.RelayResult result = relay.relayBatch();

        assertThat(result.blocked()).isTrue();
        verify(outboxRepository, never()).findByStatusOrderByIdAsc(any(), any());
        verifyNoInteractions(projector);
    }

    @Test
    @DisplayName("성공한 이벤트는 DONE으로 바꾸고, 배치가 가득 차면 다음 배치가 있다고 알린다")
    void marksDone() {
        List<ActivityOutbox> batch = List.of(outbox("A"), outbox("B"), outbox("C"));
        givenBatch(batch);

        ActivityOutboxRelay.RelayResult result = relay.relayBatch();

        assertThat(result.done()).isEqualTo(3);
        assertThat(result.hasMore()).isTrue();
        assertThat(batch).allSatisfy(outbox -> {
            assertThat(outbox.getStatus()).isEqualTo(ActivityOutboxStatus.DONE);
            assertThat(outbox.getProcessedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("MongoDB 장애면 재시도 예약 후 멈추고, 뒤 이벤트는 반영하지 않는다")
    void stopsOnInfrastructureFailure() {
        ActivityOutbox first = outbox("A");
        ActivityOutbox second = outbox("B");
        givenBatch(List.of(first, second));
        willThrow(new DataAccessResourceFailureException("mongo down"))
                .given(projector)
                .project("A");

        ActivityOutboxRelay.RelayResult result = relay.relayBatch();

        assertThat(result.blocked()).isTrue();
        assertThat(first.getStatus()).isEqualTo(ActivityOutboxStatus.PENDING);
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(first.getLastError()).contains("mongo down");
        assertThat(second.getStatus()).isEqualTo(ActivityOutboxStatus.PENDING);
        verify(projector, never()).project("B");
    }

    @Test
    @DisplayName("MongoDB 장애는 최대 시도 횟수를 넘어도 DEAD로 보내지 않는다")
    void neverGivesUpOnInfrastructureFailure() {
        ActivityOutbox outbox = outbox("A");
        for (int i = 0; i < 10; i++) {
            outbox.markRetry("mongo down", NOW);
        }
        givenBatch(List.of(outbox));
        willThrow(new DataAccessResourceFailureException("mongo down"))
                .given(projector)
                .project("A");

        relay.relayBatch();

        assertThat(outbox.getStatus()).isEqualTo(ActivityOutboxStatus.PENDING);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(10)); // maxBackoff
    }

    @Test
    @DisplayName("데이터 문제는 최대 시도 횟수에 닿으면 DEAD로 보내고 다음 이벤트로 넘어간다")
    void deadLettersPoisonEvent() {
        ActivityOutbox poison = outbox("A");
        poison.markRetry("bad", NOW);
        poison.markRetry("bad", NOW); // 이번이 3번째 시도
        ActivityOutbox next = outbox("B");
        givenBatch(List.of(poison, next));
        willThrow(new IllegalArgumentException("bad payload")).given(projector).project("A");

        ActivityOutboxRelay.RelayResult result = relay.relayBatch();

        assertThat(poison.getStatus()).isEqualTo(ActivityOutboxStatus.DEAD);
        assertThat(poison.getAttempts()).isEqualTo(3);
        assertThat(next.getStatus()).isEqualTo(ActivityOutboxStatus.DONE);
        assertThat(result.dead()).isEqualTo(1);
        assertThat(result.done()).isEqualTo(1);
    }

    @Test
    @DisplayName("데이터 문제라도 시도 횟수가 남았으면 재시도 예약 후 멈춘다")
    void retriesPoisonEventBeforeLimit() {
        ActivityOutbox poison = outbox("A");
        givenBatch(List.of(poison, outbox("B")));
        willThrow(new IllegalArgumentException("bad payload")).given(projector).project("A");

        relay.relayBatch();

        assertThat(poison.getStatus()).isEqualTo(ActivityOutboxStatus.PENDING);
        assertThat(poison.getAttempts()).isEqualTo(1);
        verify(projector, never()).project("B");
    }

    @Test
    @DisplayName("맨 앞 이벤트가 재시도 대기 중이면 뒤 이벤트도 기다린다")
    void waitsForHeadBackoff() {
        ActivityOutbox waiting = outbox("A");
        waiting.markRetry("mongo down", NOW.plusSeconds(5));
        givenBatch(List.of(waiting, outbox("B")));

        ActivityOutboxRelay.RelayResult result = relay.relayBatch();

        assertThat(result.blocked()).isTrue();
        verifyNoInteractions(projector);
    }

    @Test
    @DisplayName("재시도 대기 시간은 두 배씩 늘고 상한에서 멈춘다")
    void exponentialBackoff() {
        assertThat(properties.backoff(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.backoff(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.backoff(4)).isEqualTo(Duration.ofSeconds(8));
        assertThat(properties.backoff(5)).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.backoff(100)).isEqualTo(Duration.ofSeconds(10));
    }

    private void givenBatch(List<ActivityOutbox> batch) {
        given(outboxRepository.tryTransactionLock(anyLong())).willReturn(true);
        given(outboxRepository.findByStatusOrderByIdAsc(eq(ActivityOutboxStatus.PENDING), any(Limit.class)))
                .willReturn(batch);
        for (ActivityOutbox outbox : batch) {
            // payload를 그대로 이벤트로 쓴다
            lenient()
                    .when(codec.deserialize(outbox.getEventType(), outbox.getPayload()))
                    .thenReturn(outbox.getPayload());
        }
    }

    private static ActivityOutbox outbox(String payload) {
        return ActivityOutbox.pending("Type" + payload, payload, NOW);
    }
}

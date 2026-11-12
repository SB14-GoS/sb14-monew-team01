package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.interest.api.InterestSubscribedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class ActivityEventCodecTest {

    private final ActivityEventCodec codec =
            new ActivityEventCodec(JsonMapper.builder().build());

    @Test
    @DisplayName("이벤트를 JSON으로 저장했다가 같은 값으로 되돌린다")
    void roundTrip() {
        InterestSubscribedEvent event = new InterestSubscribedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "경제",
                List.of("주식", "금리"),
                5L,
                Instant.parse("2026-11-12T01:02:03.456789Z"));

        String type = codec.typeOf(event);
        Object restored = codec.deserialize(type, codec.serialize(event));

        assertThat(type).isEqualTo("InterestSubscribedEvent");
        assertThat(restored).isEqualTo(event);
    }

    @Test
    @DisplayName("허용 목록에 없는 타입은 거부한다")
    void rejectsUnknownType() {
        assertThatThrownBy(() -> codec.deserialize("java.lang.Runtime", "{}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.typeOf("문자열")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("깨진 JSON이나 잘못된 UUID·시각은 예외로 알린다 (relay가 데이터 문제로 처리)")
    void rejectsBrokenPayload() {
        assertThatThrownBy(() -> codec.deserialize("InterestUnsubscribedEvent", "{not json"))
                .isInstanceOf(JacksonException.class);
        assertThatThrownBy(() -> codec.deserialize(
                        "InterestUnsubscribedEvent", "{\"userId\":\"not-a-uuid\",\"interestId\":null}"))
                .isInstanceOf(JacksonException.class);
        assertThatThrownBy(() -> codec.deserialize(
                        "UserRegisteredEvent",
                        "{\"userId\":\"%s\",\"email\":\"a@b.c\",\"nickname\":\"n\",\"createdAt\":\"어제\"}"
                                .formatted(UUID.randomUUID())))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    @DisplayName("Recorder가 듣는 이벤트 목록과 허용 목록이 같다")
    void recorderListensToEveryType() throws NoSuchMethodException {
        TransactionalEventListener listener = ActivityOutboxRecorder.class
                .getMethod("record", Object.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(listener.classes()).containsExactlyInAnyOrderElementsOf(ActivityEventCodec.EVENT_TYPES);
    }
}

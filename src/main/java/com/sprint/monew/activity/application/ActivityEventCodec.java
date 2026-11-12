package com.sprint.monew.activity.application;

import com.sprint.monew.article.api.ArticleDeleteEvent;
import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.api.CommentDeletedEvent;
import com.sprint.monew.comment.api.CommentLikedEvent;
import com.sprint.monew.comment.api.CommentUnlikedEvent;
import com.sprint.monew.comment.api.CommentUpdatedEvent;
import com.sprint.monew.interest.api.InterestDeletedEvent;
import com.sprint.monew.interest.api.InterestSubscribedEvent;
import com.sprint.monew.interest.api.InterestUnsubscribedEvent;
import com.sprint.monew.user.api.UserDeletedEvent;
import com.sprint.monew.user.api.UserNicknameUpdatedEvent;
import com.sprint.monew.user.api.UserRegisteredEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * outbox에 저장할 이벤트를 JSON으로 바꾸고 되돌린다.
 *
 * <p>저장된 타입 이름으로 아무 클래스나 만들지 않도록, 활동 내역이 다루는 이벤트만 허용 목록으로 둔다.
 */
@Component
@RequiredArgsConstructor
public class ActivityEventCodec {

    /** 활동 내역이 구독하는 이벤트 전체. {@link ActivityOutboxRecorder}의 리스너 목록과 같아야 한다. */
    static final List<Class<?>> EVENT_TYPES = List.of(
            ArticleViewedEvent.class,
            ArticleDeleteEvent.class,
            InterestSubscribedEvent.class,
            InterestUnsubscribedEvent.class,
            InterestDeletedEvent.class,
            UserRegisteredEvent.class,
            UserNicknameUpdatedEvent.class,
            UserDeletedEvent.class,
            CommentCreatedEvent.class,
            CommentUpdatedEvent.class,
            CommentDeletedEvent.class,
            CommentLikedEvent.class,
            CommentUnlikedEvent.class);

    private static final Map<String, Class<?>> TYPES_BY_NAME =
            EVENT_TYPES.stream().collect(Collectors.toUnmodifiableMap(Class::getSimpleName, Function.identity()));

    private final JsonMapper jsonMapper;

    public String typeOf(Object event) {
        String name = event.getClass().getSimpleName();
        if (!TYPES_BY_NAME.containsKey(name)) {
            throw new IllegalArgumentException(
                    "활동 내역이 다루지 않는 이벤트: " + event.getClass().getName());
        }
        return name;
    }

    public String serialize(Object event) {
        return jsonMapper.writeValueAsString(event);
    }

    public Object deserialize(String eventType, String payload) {
        Class<?> type = TYPES_BY_NAME.get(eventType);
        if (type == null) {
            throw new IllegalArgumentException("알 수 없는 활동 이벤트 타입: " + eventType);
        }
        return jsonMapper.readValue(payload, type);
    }
}

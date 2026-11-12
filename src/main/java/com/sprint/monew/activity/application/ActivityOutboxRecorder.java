package com.sprint.monew.activity.application;

import com.sprint.monew.activity.domain.ActivityOutbox;
import com.sprint.monew.activity.domain.ActivityOutboxRepository;
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
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 활동 내역 이벤트를 원래 요청과 같은 트랜잭션에 outbox로 저장한다 (Transactional Outbox).
 *
 * <p>BEFORE_COMMIT이라 원본이 롤백되면 outbox도 함께 롤백되고, 커밋되면 이벤트가 반드시 남는다. MongoDB 반영은
 * {@link ActivityOutboxRelay}가 요청 스레드 밖에서 한다. 이벤트를 발행하는 모듈은 바꾸지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ActivityOutboxRecorder {

    private final ActivityOutboxRepository outboxRepository;
    private final ActivityEventCodec codec;
    private final Clock clock;

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            fallbackExecution = true,
            classes = {
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
                CommentUnlikedEvent.class
            })
    public void record(Object event) {
        outboxRepository.save(ActivityOutbox.pending(codec.typeOf(event), codec.serialize(event), clock.instant()));
    }
}

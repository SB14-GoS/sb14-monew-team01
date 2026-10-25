package com.sprint.monew.notification.infra;

import static com.sprint.monew.notification.domain.QNotification.notification;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.notification.domain.Notification;
import com.sprint.monew.notification.domain.NotificationQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory factory;

    @Override
    public List<Notification> findUnconfirmedByUserIdWithCursor(UUID userId, UUID cursorId, Instant after, int limit) {
        return factory.selectFrom(notification)
                .where(
                        notification.userId.eq(userId),
                        notification.isConfirmed.isFalse(),
                        cursorCondition(cursorId, after))
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(limit + 1L)
                .fetch();
    }

    private BooleanExpression cursorCondition(UUID cursorId, Instant after) {
        if (cursorId == null || after == null) {
            return null;
        }
        return notification
                .createdAt
                .lt(after)
                .or(notification.createdAt.eq(after).and(notification.id.lt(cursorId)));
    }
}

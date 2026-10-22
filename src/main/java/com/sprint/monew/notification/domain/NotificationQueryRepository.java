package com.sprint.monew.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationQueryRepository {

    List<Notification> findUnconfirmedByUserIdWithCursor(UUID userId, UUID cursorId, Instant after, int limit);
}

package com.sprint.monew.notification.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    long countByUserIdAndIsConfirmedFalse(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.isConfirmed = true, n.confirmedAt = :now, n.updatedAt = :now "
            + "where n.userId = :userId and n.isConfirmed = false")
    int confirmAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}

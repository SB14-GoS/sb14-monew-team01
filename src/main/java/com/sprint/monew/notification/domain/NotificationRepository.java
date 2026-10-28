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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.isConfirmed = true, n.confirmedAt = :now, n.updatedAt = :now "
            + "where n.id = :notificationId and n.userId = :userId and n.isConfirmed = false")
    int confirmByIdAndUserId(
            @Param("notificationId") UUID notificationId, @Param("userId") UUID userId, @Param("now") Instant now);

    boolean existsByIdAndUserId(UUID notificationId, UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Notification n where n.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}

package com.sprint.monew.notification.web.dto;

import com.sprint.monew.notification.domain.Notification;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        boolean confirmed,
        UUID userId,
        String content,
        String resourceType,
        UUID resourceId) {

    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.isConfirmed(),
                notification.getUserId(),
                notification.getContent(),
                notification.getResourceType().name().toLowerCase(Locale.ROOT), // 명세: "interest" / "comment"
                notification.getResourceId());
    }
}

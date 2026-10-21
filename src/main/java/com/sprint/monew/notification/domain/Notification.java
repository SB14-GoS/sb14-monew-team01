package com.sprint.monew.notification.domain;

import com.sprint.monew.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "content", nullable = false)
    String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    UUID resourceId;

    @Column(name = "is_read", nullable = false)
    boolean isRead = false;

    @Column(name = "read_at")
    Instant readAt;

    public Notification(UUID userId, String content, ResourceType resourceType, UUID resourceId) {
        this.userId = userId;
        this.content = content;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.isRead = false;
        this.readAt = null;
    }

    // 알림 확인 시 사용할 메서드
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = Instant.now();
        }
    }
}

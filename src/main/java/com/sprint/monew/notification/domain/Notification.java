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

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "is_confirmed", nullable = false)
    private boolean isConfirmed = false;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public Notification(UUID userId, String content, ResourceType resourceType, UUID resourceId) {
        this.userId = userId;
        this.content = content;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.isConfirmed = false;
        this.confirmedAt = null;
    }

    // 알림 확인 시 사용할 메서드
    public void markAsRead() {
        if (!this.isConfirmed) {
            this.isConfirmed = true;
            this.confirmedAt = Instant.now();
        }
    }
}

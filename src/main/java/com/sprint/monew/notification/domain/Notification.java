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

    // 기사 등록 알림 생성 팩토리 메서드
    public static Notification ofInterestArticle(UUID userId, UUID interestId, String interestName, long articleCount) {
        String content = String.format("%s와 관련된 기사가 %d건 등록되었습니다.", interestName, articleCount);
        return new Notification(userId, content, ResourceType.INTEREST, interestId);
    }

    // 댓글 좋아요 알림 생성 팩토리 메서드
    public static Notification ofCommentLike(UUID receiverUserId, UUID commentId, String likerNickname) {
        String content = String.format("%s님이 나의 댓글을 좋아합니다.", likerNickname);
        return new Notification(receiverUserId, content, ResourceType.COMMENT, commentId);
    }

    // 알림 확인 시 사용할 메서드
    public void markAsRead() {
        if (!this.isConfirmed) {
            this.isConfirmed = true;
            this.confirmedAt = Instant.now();
        }
    }
}

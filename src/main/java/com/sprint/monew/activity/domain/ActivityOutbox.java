package com.sprint.monew.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 활동 내역에 반영할 도메인 이벤트. 원래 요청과 같은 트랜잭션에 저장되므로 원본이 커밋되면 이벤트도 반드시 남는다.
 *
 * <p>id는 저장 순서대로 증가하며, relay는 id 순서로 반영해 같은 사용자의 이벤트 순서를 지킨다.
 */
@Entity
@Getter
@Table(name = "activity_outbox", indexes = @Index(name = "idx_activity_outbox_status_id", columnList = "status, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityOutbox {

    private static final int ERROR_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityOutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    private ActivityOutbox(String eventType, String payload, Instant now) {
        this.eventType = eventType;
        this.payload = payload;
        this.status = ActivityOutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public static ActivityOutbox pending(String eventType, String payload, Instant now) {
        return new ActivityOutbox(eventType, payload, now);
    }

    public boolean isDue(Instant now) {
        return !nextAttemptAt.isAfter(now);
    }

    public void markDone(Instant now) {
        this.status = ActivityOutboxStatus.DONE;
        this.processedAt = now;
        this.lastError = null;
    }

    public void markRetry(String error, Instant nextAttemptAt) {
        this.attempts++;
        this.lastError = truncate(error);
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markDead(String error, Instant now) {
        this.attempts++;
        this.status = ActivityOutboxStatus.DEAD;
        this.lastError = truncate(error);
        this.processedAt = now;
    }

    private static String truncate(String error) {
        if (error == null || error.length() <= ERROR_MAX_LENGTH) {
            return error;
        }
        return error.substring(0, ERROR_MAX_LENGTH);
    }
}

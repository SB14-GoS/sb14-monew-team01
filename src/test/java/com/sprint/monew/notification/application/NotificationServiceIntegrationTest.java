package com.sprint.monew.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.notification.domain.Notification;
import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.notification.domain.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// 테스트에 @Transactional을 붙이지 않는다. 서비스 트랜잭션이 실제로 커밋돼야 DB 반영 여부를 확인할 수 있다
@IntegrationTest
class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
    }

    private Notification save(UUID owner, boolean confirmed) {
        Notification notification = new Notification(owner, "알림", ResourceType.COMMENT, UUID.randomUUID());
        if (confirmed) {
            notification.markAsRead();
        }
        return notificationRepository.save(notification);
    }

    private Notification reload(Notification notification) {
        return notificationRepository.findById(notification.getId()).orElseThrow();
    }

    @Test
    @DisplayName("알림을 확인하면 확인 상태가 DB에 저장된다")
    void confirmPersists() {
        Notification notification = save(userId, false);

        notificationService.confirm(notification.getId(), userId);

        Notification found = reload(notification);
        assertThat(found.isConfirmed()).isTrue();
        assertThat(found.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 확인한 알림을 다시 확인해도 확인 시각이 바뀌지 않는다")
    void confirmIsIdempotent() {
        Notification notification = save(userId, false);
        notificationService.confirm(notification.getId(), userId);
        Instant firstConfirmedAt = reload(notification).getConfirmedAt();

        notificationService.confirm(notification.getId(), userId);

        assertThat(reload(notification).getConfirmedAt()).isEqualTo(firstConfirmedAt);
    }

    @Test
    @DisplayName("전체 확인은 내 미확인 알림만 확인 처리하고 updatedAt을 갱신한다")
    void confirmAllUpdatesOnlyMyUnconfirmed() {
        Notification mine1 = save(userId, false);
        Notification mine2 = save(userId, false);
        Notification alreadyConfirmed = save(userId, true);
        Notification others = save(UUID.randomUUID(), false);
        Instant oldUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update("update notifications set updated_at = ?", Timestamp.from(oldUpdatedAt));
        Instant alreadyConfirmedAt = reload(alreadyConfirmed).getConfirmedAt();

        notificationService.confirmAll(userId);

        for (Notification mine : new Notification[] {mine1, mine2}) {
            Notification found = reload(mine);
            assertThat(found.isConfirmed()).isTrue();
            assertThat(found.getConfirmedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isAfter(oldUpdatedAt);
        }
        Notification foundConfirmed = reload(alreadyConfirmed);
        assertThat(foundConfirmed.getConfirmedAt()).isEqualTo(alreadyConfirmedAt);
        assertThat(foundConfirmed.getUpdatedAt()).isEqualTo(oldUpdatedAt);
        Notification foundOthers = reload(others);
        assertThat(foundOthers.isConfirmed()).isFalse();
        assertThat(foundOthers.getConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("전체 확인 후 미확인 알림 개수는 0이 된다")
    void confirmAllClearsUnconfirmedCount() {
        save(userId, false);
        save(userId, false);

        notificationService.confirmAll(userId);
        notificationService.confirmAll(userId);

        assertThat(notificationRepository.countByUserIdAndIsConfirmedFalse(userId))
                .isZero();
    }
}

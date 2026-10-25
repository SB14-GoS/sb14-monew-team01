package com.sprint.monew.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.notification.domain.Notification;
import com.sprint.monew.notification.domain.NotificationQueryRepository;
import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.notification.domain.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class NotificationQueryRepositoryImplTest {

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationQueryRepository notificationQueryRepository;

    @Autowired
    EntityManager em;

    private final UUID userId = UUID.randomUUID();

    // createdAt은 auditing이 채우고 updatable = false라서, 원하는 시각은 native UPDATE로 넣는다
    private Notification save(UUID owner, Instant createdAt, boolean confirmed) {
        Notification notification = new Notification(owner, "알림", ResourceType.COMMENT, UUID.randomUUID());
        if (confirmed) {
            notification.markAsRead();
        }
        notificationRepository.saveAndFlush(notification);
        em.createNativeQuery("update notifications set created_at = :createdAt where id = :id")
                .setParameter("createdAt", Timestamp.from(createdAt))
                .setParameter("id", notification.getId())
                .executeUpdate();
        return notification;
    }

    @Test
    @DisplayName("같은 시각의 알림이 있어도 커서로 넘기면 누락·중복 없이 최신순으로 모두 조회된다")
    void paginatesWithoutGapOrDuplicate() {
        Instant sameTime = Instant.parse("2026-10-22T01:00:00Z");
        List<UUID> expected = new ArrayList<>();
        expected.add(save(userId, Instant.parse("2026-10-22T03:00:00Z"), false).getId());
        List<UUID> sameTimeIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            sameTimeIds.add(save(userId, sameTime, false).getId());
        }
        expected.add(save(userId, Instant.parse("2026-10-21T23:00:00Z"), false).getId());
        em.clear();

        int limit = 2;
        List<Notification> collected = new ArrayList<>();
        UUID cursorId = null;
        Instant after = null;
        for (int page = 0; page < 10; page++) {
            List<Notification> fetched =
                    notificationQueryRepository.findUnconfirmedByUserIdWithCursor(userId, cursorId, after, limit);
            assertThat(fetched).hasSizeLessThanOrEqualTo(limit + 1);
            List<Notification> content = fetched.subList(0, Math.min(limit, fetched.size()));
            collected.addAll(content);
            if (fetched.size() <= limit) {
                break;
            }
            Notification last = content.get(content.size() - 1);
            cursorId = last.getId();
            after = last.getCreatedAt();
        }

        assertThat(collected)
                .extracting(Notification::getId)
                .doesNotHaveDuplicates()
                .hasSize(5);
        assertThat(collected.get(0).getId()).isEqualTo(expected.get(0));
        assertThat(collected.subList(1, 4))
                .extracting(Notification::getId)
                .containsExactlyInAnyOrderElementsOf(sameTimeIds);
        assertThat(collected.get(4).getId()).isEqualTo(expected.get(1));
        assertThat(collected).extracting(Notification::getCreatedAt).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    @DisplayName("확인한 알림과 다른 사용자의 알림은 조회·개수에서 빠진다")
    void excludesConfirmedAndOtherUsers() {
        Notification mine = save(userId, Instant.parse("2026-10-22T02:00:00Z"), false);
        save(userId, Instant.parse("2026-10-22T03:00:00Z"), true);
        save(UUID.randomUUID(), Instant.parse("2026-10-22T04:00:00Z"), false);
        em.clear();

        List<Notification> fetched =
                notificationQueryRepository.findUnconfirmedByUserIdWithCursor(userId, null, null, 10);

        assertThat(fetched).extracting(Notification::getId).containsExactly(mine.getId());
        assertThat(notificationRepository.countByUserIdAndIsConfirmedFalse(userId))
                .isEqualTo(1L);
    }
}

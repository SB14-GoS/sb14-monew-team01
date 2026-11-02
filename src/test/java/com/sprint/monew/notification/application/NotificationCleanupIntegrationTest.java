package com.sprint.monew.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.notification.domain.Notification;
import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.notification.domain.ResourceType;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class NotificationCleanupIntegrationTest {
    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JobOperator jobOperator;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    @Qualifier("notificationCleanupJob")
    Job cleanupJob;

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("확인 시각이 7일을 초과한 알림만 삭제한다")
    void deletesOnlyNotificationsConfirmedBeforeCutoff() {
        Instant reference = Instant.parse("2026-11-02T00:00:00Z");
        Instant cutoff = reference.minus(7, ChronoUnit.DAYS);
        Notification expired = save(true, cutoff.minusSeconds(1));
        Notification boundary = save(true, cutoff);
        Notification recent = save(true, cutoff.plusSeconds(1));
        Notification unread = save(false, null);
        // 생성된 지 오래됐어도 확인한 지 7일이 지나지 않으면 보존한다.
        jdbcTemplate.update(
                "update notifications set created_at = ? where id = ?",
                Timestamp.from(cutoff.minus(30, ChronoUnit.DAYS)),
                recent.getId());

        assertThat(notificationService.deleteExpireNotification(reference)).isEqualTo(1);
        assertThat(notificationRepository.findAll())
                .extracting(Notification::getId)
                .containsExactlyInAnyOrder(boundary.getId(), recent.getId(), unread.getId());
        assertThat(notificationService.deleteExpireNotification(reference)).isZero();
    }

    @Test
    @DisplayName("정리 배치는 요청 시각을 기준으로 삭제하고 삭제 건수를 Job 실행에 기록한다")
    void jobDeletesExpiredNotifications() throws Exception {
        Instant requestedAt = Instant.parse("2020-01-15T00:00:00Z");
        Instant cutoff = requestedAt.minus(7, ChronoUnit.DAYS);
        Notification expired = save(true, cutoff.minusSeconds(1));
        Notification boundary = save(true, cutoff);
        Notification recent = save(true, cutoff.plusSeconds(1));
        Notification unread = save(false, null);

        var execution = jobOperator.start(
                cleanupJob,
                new JobParametersBuilder()
                        .addString("requestedAt", requestedAt.toString())
                        .addString("testRun", UUID.randomUUID().toString())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        var storedExecution = jobRepository.getJobExecution(execution.getId());
        assertThat(storedExecution).isNotNull();
        assertThat(storedExecution.getExecutionContext().getInt("deletedCount")).isEqualTo(1);
        assertThat(notificationRepository.existsById(expired.getId())).isFalse();
        assertThat(notificationRepository.findAll())
                .extracting(Notification::getId)
                .containsExactlyInAnyOrder(boundary.getId(), recent.getId(), unread.getId());
    }

    private Notification save(boolean confirmed, Instant confirmedAt) {
        Notification notification = new Notification(UUID.randomUUID(), "알림", ResourceType.COMMENT, UUID.randomUUID());
        if (confirmed) {
            notification.markAsRead();
        }
        notification = notificationRepository.saveAndFlush(notification);
        if (confirmedAt != null) {
            jdbcTemplate.update(
                    "update notifications set confirmed_at = ? where id = ?",
                    Timestamp.from(confirmedAt),
                    notification.getId());
        }
        return notification;
    }
}

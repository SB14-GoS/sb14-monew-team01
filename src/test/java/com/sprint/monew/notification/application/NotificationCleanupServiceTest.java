package com.sprint.monew.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.monew.notification.domain.NotificationRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupServiceTest {
    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotificationService notificationService;

    @Test
    @DisplayName("기준 시각의 7일 전을 삭제 기준으로 전달하고 삭제 건수를 반환한다")
    void usesSevenDayCutoff() {
        Instant reference = Instant.parse("2026-11-02T12:34:56Z");
        Instant cutoff = Instant.parse("2026-10-26T12:34:56Z");
        given(notificationRepository.deleteConfirmedBefore(cutoff)).willReturn(3);

        assertThat(notificationService.deleteExpireNotification(reference)).isEqualTo(3);
    }
}

package com.sprint.monew.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.notification.domain.Notification;
import com.sprint.monew.notification.domain.NotificationErrorCode;
import com.sprint.monew.notification.domain.NotificationQueryRepository;
import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.notification.domain.ResourceType;
import com.sprint.monew.notification.domain.exception.NotificationNotFoundException;
import com.sprint.monew.notification.web.dto.NotificationDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    NotificationQueryRepository notificationQueryRepository;

    @InjectMocks
    NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();

    private Notification notification(ResourceType resourceType, Instant createdAt) {
        Notification notification = new Notification(userId, "알림 내용", resourceType, UUID.randomUUID());
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        ReflectionTestUtils.setField(notification, "updatedAt", createdAt);
        return notification;
    }

    @Test
    @DisplayName("첫 페이지는 커서 없이 조회하고, limit 이하로 오면 다음 페이지가 없다")
    void firstPageWithoutNext() {
        Notification only = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T01:00:00Z"));
        given(notificationQueryRepository.findUnconfirmedByUserIdWithCursor(userId, null, null, 2))
                .willReturn(List.of(only));
        given(notificationRepository.countByUserIdAndIsConfirmedFalse(userId)).willReturn(1L);

        CursorPageResponse<NotificationDto> result =
                notificationService.getNotifications(new CursorRequest(null, null, 2), userId);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextAfter()).isNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("limit + 1개가 오면 limit개만 담고, 마지막 항목으로 다음 커서를 만든다")
    void hasNextWhenMoreThanLimit() {
        Notification first = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T03:00:00Z"));
        Notification second = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T02:00:00Z"));
        Notification extra = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T01:00:00Z"));
        given(notificationQueryRepository.findUnconfirmedByUserIdWithCursor(userId, null, null, 2))
                .willReturn(List.of(first, second, extra));
        given(notificationRepository.countByUserIdAndIsConfirmedFalse(userId)).willReturn(5L);

        CursorPageResponse<NotificationDto> result =
                notificationService.getNotifications(new CursorRequest(null, null, 2), userId);

        assertThat(result.content()).extracting(NotificationDto::id).containsExactly(first.getId(), second.getId());
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(second.getId().toString());
        assertThat(result.nextAfter()).isEqualTo(second.getCreatedAt());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("cursor 문자열을 UUID로 바꿔 after와 함께 조회 조건으로 넘긴다")
    void passesParsedCursor() {
        UUID cursorId = UUID.randomUUID();
        Instant after = Instant.parse("2026-10-22T02:00:00Z");
        given(notificationQueryRepository.findUnconfirmedByUserIdWithCursor(userId, cursorId, after, 2))
                .willReturn(List.of());

        CursorPageResponse<NotificationDto> result =
                notificationService.getNotifications(new CursorRequest(cursorId.toString(), after, 2), userId);

        verify(notificationQueryRepository).findUnconfirmedByUserIdWithCursor(userId, cursorId, after, 2);
        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("cursor가 UUID 형식이 아니면 INCORRECT_REQUEST 예외를 던지고 조회하지 않는다")
    void invalidCursor() {
        CursorRequest request = new CursorRequest("not-a-uuid", Instant.now(), 2);

        assertThatThrownBy(() -> notificationService.getNotifications(request, userId))
                .isInstanceOf(MonewException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INCORRECT_REQUEST);
        verify(notificationQueryRepository, never()).findUnconfirmedByUserIdWithCursor(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("응답 DTO는 confirmed를 옮기고 resourceType을 소문자로 바꾼다")
    void mapsToDto() {
        Notification interest = notification(ResourceType.INTEREST, Instant.parse("2026-10-22T02:00:00Z"));
        Notification comment = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T01:00:00Z"));
        given(notificationQueryRepository.findUnconfirmedByUserIdWithCursor(userId, null, null, 10))
                .willReturn(List.of(interest, comment));
        given(notificationRepository.countByUserIdAndIsConfirmedFalse(userId)).willReturn(2L);

        CursorPageResponse<NotificationDto> result =
                notificationService.getNotifications(new CursorRequest(null, null, 10), userId);

        NotificationDto dto = result.content().get(0);
        assertThat(dto.id()).isEqualTo(interest.getId());
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.content()).isEqualTo("알림 내용");
        assertThat(dto.confirmed()).isFalse();
        assertThat(dto.resourceId()).isEqualTo(interest.getResourceId());
        assertThat(result.content()).extracting(NotificationDto::resourceType).containsExactly("interest", "comment");
    }

    @Test
    @DisplayName("내 알림을 확인하면 확인 상태와 확인 시각이 채워진다")
    void confirmOwnNotification() {
        Notification notification = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T01:00:00Z"));
        given(notificationRepository.findById(notification.getId())).willReturn(Optional.of(notification));

        notificationService.confirm(notification.getId(), userId);

        assertThat(notification.isConfirmed()).isTrue();
        assertThat(notification.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 사용자의 알림을 확인하면 NOTIFICATION_NOT_FOUND 예외를 던지고 상태를 바꾸지 않는다")
    void confirmOthersNotification() {
        Notification notification = notification(ResourceType.COMMENT, Instant.parse("2026-10-22T01:00:00Z"));
        given(notificationRepository.findById(notification.getId())).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.confirm(notification.getId(), UUID.randomUUID()))
                .isInstanceOf(NotificationNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        assertThat(notification.isConfirmed()).isFalse();
        assertThat(notification.getConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("없는 알림을 확인하면 NOTIFICATION_NOT_FOUND 예외를 던진다")
    void confirmMissingNotification() {
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.confirm(notificationId, userId))
                .isInstanceOf(NotificationNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 확인은 사용자 ID로 벌크 업데이트를 호출한다")
    void confirmAll() {
        notificationService.confirmAll(userId);

        verify(notificationRepository).confirmAllByUserId(eq(userId), any(Instant.class));
    }
}

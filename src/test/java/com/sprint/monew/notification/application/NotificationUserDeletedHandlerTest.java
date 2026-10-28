package com.sprint.monew.notification.application;

import static org.mockito.Mockito.verify;

import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationUserDeletedHandlerTest {

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotificationUserDeletedHandler handler;

    @Test
    @DisplayName("사용자가 삭제되면 그 사용자의 알림을 지운다")
    void onUserDeleted() {
        UUID userId = UUID.randomUUID();

        handler.onUserDeleted(new UserDeletedEvent(userId));

        verify(notificationRepository).deleteAllByUserId(userId);
    }
}

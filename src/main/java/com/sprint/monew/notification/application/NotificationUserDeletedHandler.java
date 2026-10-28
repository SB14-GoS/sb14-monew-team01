package com.sprint.monew.notification.application;

import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationUserDeletedHandler {
    private final NotificationRepository notificationRepository;

    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        notificationRepository.deleteAllByUserId(event.userId());
    }
}

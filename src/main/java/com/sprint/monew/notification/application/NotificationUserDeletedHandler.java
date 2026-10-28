package com.sprint.monew.notification.application;

import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationUserDeletedHandler {
    private final NotificationRepository notificationRepository;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        notificationRepository.deleteAllByUserId(event.userId());
    }
}

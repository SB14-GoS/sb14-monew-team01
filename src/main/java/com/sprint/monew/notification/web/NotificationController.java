package com.sprint.monew.notification.web;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.notification.application.NotificationService;
import com.sprint.monew.notification.web.dto.NotificationDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApiDocs {

    private final NotificationService notificationService;

    @Override
    @GetMapping("/api/notifcations")
    public ResponseEntity<CursorPageResponse<NotificationDto>> getNotifications(CursorRequest request, UUID userId) {

        CursorPageResponse<NotificationDto> notificationDto = notificationService.getNotifications();

        return ResponseEntity.ok(notificationDto);
    }
}

package com.sprint.monew.notification.web;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.web.RequestUserId;
import com.sprint.monew.notification.application.NotificationService;
import com.sprint.monew.notification.web.dto.NotificationDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApiDocs {

    private final NotificationService notificationService;

    @Override
    @GetMapping
    public ResponseEntity<CursorPageResponse<NotificationDto>> getNotifications(
            @Valid @ModelAttribute CursorRequest request, @RequestUserId UUID userId) {

        CursorPageResponse<NotificationDto> notificationDto = notificationService.getNotifications(request, userId);

        return ResponseEntity.ok(notificationDto);
    }
}

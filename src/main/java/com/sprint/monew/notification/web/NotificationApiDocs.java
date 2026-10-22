package com.sprint.monew.notification.web;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.notification.web.dto.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "알림 관리")
public interface NotificationApiDocs {

    @Operation(summary = "알림 목록 조회")
    ResponseEntity<CursorPageResponse<NotificationDto>> getNotifications(CursorRequest request, UUID userId);
}

package com.sprint.monew.notification.application;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.notification.domain.NotificationQueryRepository;
import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.notification.web.dto.NotificationDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;

    public CursorPageResponse<NotificationDto> getNotifications(CursorRequest request, UUID userId) {
        UUID cursorId = parseCursor(request.cursor());

        List<NotificationDto> notificationDtos =
                notificationQueryRepository
                        .findUnconfirmedByUserIdWithCursor(userId, cursorId, request.after(), request.limit())
                        .stream()
                        .map(NotificationDto::from)
                        .toList();

        long total = notificationRepository.countByUserIdAndIsConfirmedFalse(userId);

        return CursorPageResponse.of(
                notificationDtos,
                request.limit(),
                total,
                notificationDto -> notificationDto.id().toString(),
                NotificationDto::createdAt);
    }

    private UUID parseCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException exception) {
            throw new MonewException(CommonErrorCode.INCORRECT_REQUEST, Map.of("cursor", cursor));
        }
    }
}

package com.sprint.monew.notification.application;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.interest.api.InterestQueryApi;
import com.sprint.monew.notification.domain.Notification;
import com.sprint.monew.notification.domain.NotificationQueryRepository;
import com.sprint.monew.notification.domain.NotificationRepository;
import com.sprint.monew.notification.domain.exception.NotificationNotFoundException;
import com.sprint.monew.notification.web.dto.NotificationDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final InterestQueryApi interestQueryApi;

    public CursorPageResponse<NotificationDto> getNotifications(CursorRequest request, UUID userId) {
        UUID cursorId = parseCursor(request.cursor());

        if ((request.cursor() == null) != (request.after() == null)) {
            throw new MonewException(CommonErrorCode.INCORRECT_REQUEST, Map.of("field", "cursor/after"));
        }
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

    @Transactional
    public void confirm(UUID notificationId, UUID userId) {
        int updated = notificationRepository.confirmByIdAndUserId(notificationId, userId, Instant.now());
        if (updated == 0 && !notificationRepository.existsByIdAndUserId(notificationId, userId)) {
            throw new NotificationNotFoundException(notificationId);
        }
    }

    @Transactional
    public void confirmAll(UUID userId) {
        notificationRepository.confirmAllByUserId(userId, Instant.now());
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

    // 기사 등록 알림 일괄 생성 로직
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInterestArticleNotifications(UUID interestId, String interestName, long articleCount) {
        if (articleCount <= 0) {
            return;
        }

        List<UUID> subscriberIds = interestQueryApi.getSubscribedUserIds(interestId);
        if (subscriberIds.isEmpty()) {
            return;
        }

        List<Notification> notifications = subscriberIds.stream()
                .map(userId -> Notification.ofInterestArticle(userId, interestId, interestName, articleCount))
                .toList();

        notificationRepository.saveAll(notifications);
    }

    // 댓글 좋아요 단건 생성 로직
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createCommentLikeNotification(UUID receiverUserId, UUID commentId, String likerNickname) {
        Notification notification = Notification.ofCommentLike(receiverUserId, commentId, likerNickname);
        notificationRepository.save(notification);
    }
}

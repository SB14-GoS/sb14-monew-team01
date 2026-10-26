package com.sprint.monew.notification.application;

import com.sprint.monew.article.api.ArticlesCollectedEvent;
import com.sprint.monew.comment.api.CommentLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentLiked(CommentLikedEvent event) {
        if (event.commentUserId().equals(event.likerId())) {
            return;
        }
        try {
            notificationService.createCommentLikeNotification(
                    event.commentUserId(), event.commentId(), event.likerNickname());
        } catch (Exception e) {
            log.error("댓글 좋아요 알림 생성 실패 - commentId: {}, likerId: {}", event.commentId(), event.likerId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleArticlesCollected(ArticlesCollectedEvent event) {
        try {
            notificationService.createInterestArticleNotifications(
                    event.interestId(), event.interestName(), event.articleCount());
        } catch (Exception e) {
            log.error("관심사 기사 수집 알림 생성 실패 - interestId: {}", event.interestId(), e);
        }
    }
}

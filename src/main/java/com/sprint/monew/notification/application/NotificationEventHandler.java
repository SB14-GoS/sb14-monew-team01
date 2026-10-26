package com.sprint.monew.notification.application;

import com.sprint.monew.article.api.ArticlesCollectedEvent;
import com.sprint.monew.comment.api.CommentLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {
    private final NotificationService notificationService;

    @EventListener
    public void handleCommentLiked(CommentLikedEvent event) {
        if (event.commentUserId().equals(event.likerId())) {
            return;
        }

        notificationService.createCommentLikeNotification(
                event.commentUserId(), event.commentId(), event.likerNickname());
    }

    @EventListener
    public void handleArticlesCollected(ArticlesCollectedEvent event) {
        notificationService.createInterestArticleNotifications(
                event.interestId(), event.interestName(), event.articleCount());
    }
}

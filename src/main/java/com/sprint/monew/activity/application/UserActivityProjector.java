package com.sprint.monew.activity.application;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.article.api.ArticleDeleteEvent;
import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.api.CommentDeletedEvent;
import com.sprint.monew.comment.api.CommentLikedEvent;
import com.sprint.monew.comment.api.CommentUnlikedEvent;
import com.sprint.monew.comment.api.CommentUpdatedEvent;
import com.sprint.monew.interest.api.InterestDeletedEvent;
import com.sprint.monew.interest.api.InterestSubscribedEvent;
import com.sprint.monew.interest.api.InterestUnsubscribedEvent;
import com.sprint.monew.user.api.UserDeletedEvent;
import com.sprint.monew.user.api.UserNicknameUpdatedEvent;
import com.sprint.monew.user.api.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트를 MongoDB 활동 내역 문서 변경으로 옮긴다. {@link ActivityOutboxRelay}가 outbox 순서대로 호출한다.
 *
 * <p>Writer 예외는 삼키지 않고 그대로 던진다. relay가 실패를 보고 재시도한다.
 */
@Component
@RequiredArgsConstructor
public class UserActivityProjector {

    private final UserActivityWriter userActivityWriter;

    /**
     * @param sequence 이벤트 순번 (outbox id). 값을 덮어쓰는 갱신(좋아요 수)의 버전으로 쓴다.
     */
    public void project(Object event, long sequence) {
        if (event instanceof ArticleViewedEvent e) {
            onArticleViewed(e);
        } else if (event instanceof ArticleDeleteEvent e) {
            onArticleDeleted(e);
        } else if (event instanceof InterestSubscribedEvent e) {
            onInterestSubscribed(e);
        } else if (event instanceof InterestUnsubscribedEvent e) {
            onInterestUnsubscribed(e);
        } else if (event instanceof InterestDeletedEvent e) {
            onInterestDeleted(e);
        } else if (event instanceof UserRegisteredEvent e) {
            onUserRegistered(e);
        } else if (event instanceof UserNicknameUpdatedEvent e) {
            onUserNicknameUpdated(e);
        } else if (event instanceof UserDeletedEvent e) {
            onUserDeleted(e);
        } else if (event instanceof CommentCreatedEvent e) {
            onCommentCreated(e);
        } else if (event instanceof CommentUpdatedEvent e) {
            onCommentUpdated(e);
        } else if (event instanceof CommentDeletedEvent e) {
            onCommentDeleted(e);
        } else if (event instanceof CommentLikedEvent e) {
            onCommentLiked(e, sequence);
        } else if (event instanceof CommentUnlikedEvent e) {
            onCommentUnliked(e, sequence);
        } else {
            throw new IllegalArgumentException(
                    "활동 내역이 다루지 않는 이벤트: " + event.getClass().getName());
        }
    }

    public void onArticleViewed(ArticleViewedEvent event) {
        userActivityWriter.addArticleView(
                event.userId(),
                new ArticleViewItem(
                        event.articleViewId(),
                        event.userId(),
                        event.viewedAt(),
                        event.articleId(),
                        event.source(),
                        event.sourceUrl(),
                        event.articleTitle(),
                        event.articlePublishedDate(),
                        event.articleSummary(),
                        event.articleCommentCount(),
                        event.articleViewCount()));
    }

    public void onInterestSubscribed(InterestSubscribedEvent event) {
        userActivityWriter.addSubscription(
                event.userId(),
                new SubscriptionItem(
                        event.subscriptionId(),
                        event.interestId(),
                        event.interestName(),
                        event.interestKeywords(),
                        event.interestSubscriberCount(),
                        event.subscribedAt()));
    }

    public void onInterestUnsubscribed(InterestUnsubscribedEvent event) {
        userActivityWriter.removeSubscription(event.userId(), event.interestId());
    }

    public void onInterestDeleted(InterestDeletedEvent event) {
        userActivityWriter.removeInterestFromAll(event.interestId());
    }

    public void onUserRegistered(UserRegisteredEvent event) {
        userActivityWriter.create(event.userId(), event.email(), event.nickname(), event.createdAt());
    }

    public void onUserNicknameUpdated(UserNicknameUpdatedEvent event) {
        userActivityWriter.updateNickname(event.userId(), event.nickname());
    }

    public void onUserDeleted(UserDeletedEvent event) {
        userActivityWriter.delete(event.userId());
        userActivityWriter.removeCommentLikesOfUserFromAll(event.userId());
    }

    public void onCommentCreated(CommentCreatedEvent event) {
        userActivityWriter.addComment(
                event.userId(),
                new CommentItem(
                        event.commentId(),
                        event.articleId(),
                        event.articleTitle(),
                        event.userId(),
                        event.userNickname(),
                        event.content(),
                        event.likeCount(),
                        event.createdAt()));
    }

    public void onCommentUpdated(CommentUpdatedEvent event) {
        userActivityWriter.updateCommentContent(event.userId(), event.commentId(), event.content());
    }

    public void onCommentDeleted(CommentDeletedEvent event) {
        userActivityWriter.removeComment(event.userId(), event.commentId());
        userActivityWriter.removeCommentLikeFromAll(event.commentId());
    }

    public void onCommentLiked(CommentLikedEvent event, long sequence) {
        userActivityWriter.addCommentLike(
                event.likerId(),
                new CommentLikeItem(
                        event.commentLikeId(),
                        event.likedAt(),
                        event.commentId(),
                        event.articleId(),
                        event.articleTitle(),
                        event.commentUserId(),
                        event.commentUserNickname(),
                        event.commentContent(),
                        event.commentLikeCount(),
                        event.commentCreatedAt()));
        userActivityWriter.updateCommentLikeCount(
                event.commentUserId(), event.commentId(), event.commentLikeCount(), sequence);
    }

    public void onCommentUnliked(CommentUnlikedEvent event, long sequence) {
        userActivityWriter.removeCommentLike(event.likerId(), event.commentId());
        userActivityWriter.updateCommentLikeCount(
                event.commentUserId(), event.commentId(), event.commentLikeCount(), sequence);
    }

    public void onArticleDeleted(ArticleDeleteEvent event) {
        userActivityWriter.removeArticleViewFromAll(event.articleId());
    }
}

package com.sprint.monew.activity.application;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityWriter;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityProjector {

    private final UserActivityWriter userActivityWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleViewed(ArticleViewedEvent event) {
        try {
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
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "ArticleViewed", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterestSubscribed(InterestSubscribedEvent event) {
        try {
            userActivityWriter.addSubscription(
                    event.userId(),
                    new SubscriptionItem(
                            event.subscriptionId(),
                            event.interestId(),
                            event.interestName(),
                            event.interestKeywords(),
                            event.interestSubscriberCount(),
                            event.subscribedAt()));
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "InterestSubscribed", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterestUnsubscribed(InterestUnsubscribedEvent event) {
        try {
            userActivityWriter.removeSubscription(event.userId(), event.interestId());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "InterestUnsubscribed", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterestDeleted(InterestDeletedEvent event) {
        try {
            userActivityWriter.removeInterestFromAll(event.interestId());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, interestId: {}", "InterestDeleted", event.interestId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            userActivityWriter.create(event.userId(), event.email(), event.nickname(), event.createdAt());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "UserRegistered", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserNicknameUpdated(UserNicknameUpdatedEvent event) {
        try {
            userActivityWriter.updateNickname(event.userId(), event.nickname());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "UserNicknameUpdated", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            userActivityWriter.delete(event.userId());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "UserDeleted", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        try {
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
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentCreated", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentUpdated(CommentUpdatedEvent event) {
        try {
            userActivityWriter.updateCommentContent(event.userId(), event.commentId(), event.content());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentUpdated", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentDeleted(CommentDeletedEvent event) {
        try {
            userActivityWriter.removeComment(event.userId(), event.commentId());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentDeleted", event.userId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentLiked(CommentLikedEvent event) {
        try {
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
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentLiked", event.likerId(), exception);
        }
        try {
            userActivityWriter.updateCommentLikeCount(
                    event.commentUserId(), event.commentId(), event.commentLikeCount());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentLikedCount", event.commentUserId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentUnliked(CommentUnlikedEvent event) {
        try {
            userActivityWriter.removeCommentLike(event.likerId(), event.commentId());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentUnliked", event.likerId(), exception);
        }
        try {
            userActivityWriter.updateCommentLikeCount(
                    event.commentUserId(), event.commentId(), event.commentLikeCount());
        } catch (Exception exception) {
            log.error("활동 내역 갱신 실패 - event: {}, userId: {}", "CommentUnlikedCount", event.commentUserId(), exception);
        }
    }
}

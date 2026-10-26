package com.sprint.monew.activity.domain;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import java.time.Instant;
import java.util.UUID;

public interface UserActivityWriter {

    void create(UUID userId, String email, String nickname, Instant createdAt);

    void updateNickname(UUID userId, String nickname);

    void delete(UUID userId);

    void addArticleView(UUID userId, ArticleViewItem item);

    void addComment(UUID userId, CommentItem item);

    void updateCommentContent(UUID userId, UUID commentId, String content);

    void removeComment(UUID userId, UUID commentId);

    void addSubscription(UUID userId, SubscriptionItem item);

    void removeSubscription(UUID userId, UUID interestId);

    void removeInterestFromAll(UUID interestId);

    void addCommentLike(UUID likerId, CommentLikeItem item);

    void removeCommentLike(UUID likerId, UUID commentId);

    void updateCommentLikeCount(UUID commentUserId, UUID commentId, long likeCount);
}

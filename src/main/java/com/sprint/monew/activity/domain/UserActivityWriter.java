package com.sprint.monew.activity.domain;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import java.time.Instant;
import java.util.UUID;

public interface UserActivityWriter {

    void create(UUID userId, String email, String nickname, Instant createdAt);

    /**
     * 원본으로 다시 만든 문서를 한 번에 넣는다. 이미 문서가 있으면 건드리지 않는다.
     *
     * @return 새로 넣었으면 true, 이미 있었으면 false
     */
    boolean restore(UserActivitySource source);

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

    /**
     * @param version 이벤트 순번 (outbox id). 저장된 버전보다 클 때만 반영해 순서가 뒤바뀐 이벤트가 최신 값을 덮지 않게 한다.
     */
    void updateCommentLikeCount(UUID commentUserId, UUID commentId, long likeCount, long version);

    void removeCommentLikeFromAll(UUID commentId);

    void removeArticleView(UUID userId, UUID articleViewId);

    void removeArticleViewFromAll(UUID articleId);

    void removeCommentLikesOfUserFromAll(UUID commentUserId);
}

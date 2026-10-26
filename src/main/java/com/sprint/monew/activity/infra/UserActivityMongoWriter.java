package com.sprint.monew.activity.infra;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityWriter;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityMongoWriter implements UserActivityWriter {

    private static final int RECENT_LIMIT = 10;
    private final MongoTemplate mongoTemplate;

    @Override
    public void create(UUID userId, String email, String nickname, Instant createdAt) {}

    @Override
    public void updateNickname(UUID userId, String nickname) {}

    @Override
    public void delete(UUID userId) {}

    @Override
    public void addArticleView(UUID userId, ArticleViewItem item) {}

    @Override
    public void addComment(UUID userId, CommentItem item) {}

    @Override
    public void updateCommentContent(UUID userId, UUID commentId, String content) {}

    @Override
    public void removeComment(UUID userId, UUID commentId) {}

    @Override
    public void addSubscription(UUID userId, SubscriptionItem item) {}

    @Override
    public void removeSubscription(UUID userId, UUID interestId) {}

    @Override
    public void removeInterestFromAll(UUID interestId) {}

    @Override
    public void addCommentLike(UUID likerId, CommentLikeItem item) {}

    @Override
    public void removeCommentLike(UUID likerId, UUID commentId) {}

    @Override
    public void updateCommentLikeCount(UUID commentUserId, UUID commentId, long likeCount) {}
}

package com.sprint.monew.activity.infra;

import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityWriter;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityMongoWriter implements UserActivityWriter {

    private static final int RECENT_LIMIT = 10;
    private final MongoTemplate mongoTemplate;

    @Override
    public void create(UUID userId, String email, String nickname, Instant createdAt) {
        mongoTemplate.insert(new UserActivityDocument(userId, email, nickname, createdAt));
    }

    @Override
    public void updateNickname(UUID userId, String nickname) {
        Update update = new Update().set("nickname", nickname);
        mongoTemplate.updateFirst(byUser(userId), update, UserActivityDocument.class);
    }

    @Override
    public void delete(UUID userId) {
        mongoTemplate.remove(byUser(userId), UserActivityDocument.class);
    }

    @Override
    public void addArticleView(UUID userId, ArticleViewItem item) {
        pushRecent(userId, "articleViews", item);
    }

    @Override
    public void addComment(UUID userId, CommentItem item) {
        pushRecent(userId, "comments", item);
    }

    @Override
    public void updateCommentContent(UUID userId, UUID commentId, String content) {
        Query query = Query.query(
                Criteria.where("_id").is(userId).and("comments.commentId").is(commentId));
        Update update = new Update().set("comments.$.content", content);
        mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
    }

    @Override
    public void removeComment(UUID userId, UUID commentId) {
        Update update = new Update().pull("comments", new Document("commentId", commentId));
        mongoTemplate.updateFirst(byUser(userId), update, UserActivityDocument.class);
    }

    @Override
    public void addSubscription(UUID userId, SubscriptionItem item) {
        Update update = new Update().push("subscriptions").atPosition(0).each(item);
        mongoTemplate.updateFirst(byUser(userId), update, UserActivityDocument.class);
    }

    @Override
    public void removeSubscription(UUID userId, UUID interestId) {
        Update update = new Update().pull("subscriptions", new Document("interestId", interestId));
        mongoTemplate.updateFirst(byUser(userId), update, UserActivityDocument.class);
    }

    @Override
    public void removeInterestFromAll(UUID interestId) {
        Query query = Query.query(Criteria.where("subscriptions.interestId").is(interestId));
        Update update = new Update().pull("subscriptions", new Document("interestId", interestId));
        mongoTemplate.updateMulti(query, update, UserActivityDocument.class);
    }

    @Override
    public void addCommentLike(UUID likerId, CommentLikeItem item) {
        pushRecent(likerId, "commentLikes", item);
    }

    @Override
    public void removeCommentLike(UUID likerId, UUID commentId) {
        Update update = new Update().pull("commentLikes", new Document("commentId", commentId));
        mongoTemplate.updateFirst(byUser(likerId), update, UserActivityDocument.class);
    }

    @Override
    public void updateCommentLikeCount(UUID commentUserId, UUID commentId, long likeCount) {
        Query query = Query.query(Criteria.where("_id")
                .is(commentUserId)
                .and("comments.commentId")
                .is(commentId));
        Update update = new Update().set("comments.$.likeCount", likeCount);
        mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
    }

    private void pushRecent(UUID userId, String field, Object item) {
        Update update =
                new Update().push(field).atPosition(0).slice(RECENT_LIMIT).each(item);
        mongoTemplate.updateFirst(byUser(userId), update, UserActivityDocument.class);
    }

    private Query byUser(UUID userId) {
        return Query.query(Criteria.where("_id").is(userId));
    }
}

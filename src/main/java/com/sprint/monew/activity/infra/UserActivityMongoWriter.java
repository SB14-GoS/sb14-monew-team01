package com.sprint.monew.activity.infra;

import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityMongoWriter implements UserActivityWriter {

    private static final int RECENT_LIMIT = 10;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");
    static final String LIKE_COUNT_VERSION = "likeCountVersion";
    private final MongoTemplate mongoTemplate;

    /** 같은 가입 이벤트가 다시 반영돼도 기존 문서를 건드리지 않는다 ($setOnInsert). */
    @Override
    public void create(UUID userId, String email, String nickname, Instant createdAt) {
        Update update = new Update()
                .setOnInsert("email", email)
                .setOnInsert("nickname", nickname)
                .setOnInsert("createdAt", createdAt)
                .setOnInsert("subscriptions", List.of())
                .setOnInsert("comments", List.of())
                .setOnInsert("commentLikes", List.of())
                .setOnInsert("articleViews", List.of());
        mongoTemplate.upsert(byUser(userId), update, UserActivityDocument.class);
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
        pushRecent(userId, "articleViews", "articleViewId", item.articleViewId(), item);
    }

    @Override
    public void addComment(UUID userId, CommentItem item) {
        pushRecent(userId, "comments", "commentId", item.commentId(), item);
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
        Query query = notContaining(userId, "subscriptions", "interestId", item.interestId());
        Update update = new Update().push("subscriptions").sort(LATEST_FIRST).each(item);
        mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
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
        pushRecent(likerId, "commentLikes", "commentLikeId", item.commentLikeId(), item);
    }

    @Override
    public void removeCommentLike(UUID likerId, UUID commentId) {
        Update update = new Update().pull("commentLikes", new Document("commentId", commentId));
        mongoTemplate.updateFirst(byUser(likerId), update, UserActivityDocument.class);
    }

    /**
     * 좋아요 수는 이벤트에 담긴 값으로 덮어쓰므로, 저장된 버전보다 새 이벤트일 때만 바꾼다.
     *
     * <p>버전은 문서에만 저장하는 필드({@value #LIKE_COUNT_VERSION})이며 조회 모델에는 나오지 않는다. 같은 버전이 다시 오면 무시한다.
     */
    @Override
    public void updateCommentLikeCount(UUID commentUserId, UUID commentId, long likeCount, long version) {
        Criteria olderVersion = new Criteria()
                .orOperator(
                        Criteria.where(LIKE_COUNT_VERSION).lt(version),
                        Criteria.where(LIKE_COUNT_VERSION).exists(false));
        Query query = Query.query(Criteria.where("_id")
                .is(commentUserId)
                .and("comments")
                .elemMatch(Criteria.where("commentId").is(commentId).andOperator(olderVersion)));
        Update update =
                new Update().set("comments.$.likeCount", likeCount).set("comments.$." + LIKE_COUNT_VERSION, version);
        mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
    }

    @Override
    public void removeCommentLikeFromAll(UUID commentId) {
        Query query = Query.query(Criteria.where("commentLikes.commentId").is(commentId));
        Update update = new Update().pull("commentLikes", new Document("commentId", commentId));
        mongoTemplate.updateMulti(query, update, UserActivityDocument.class);
    }

    @Override
    public void removeArticleViewFromAll(UUID articleId) {
        Query query = Query.query(Criteria.where("articleViews.articleId").is(articleId));
        Update update = new Update().pull("articleViews", new Document("articleId", articleId));
        mongoTemplate.updateMulti(query, update, UserActivityDocument.class);
    }

    @Override
    public void removeCommentLikesOfUserFromAll(UUID commentUserId) {
        Query query = Query.query(Criteria.where("commentLikes.commentUserId").is(commentUserId));
        Update update = new Update().pull("commentLikes", new Document("commentUserId", commentUserId));
        mongoTemplate.updateMulti(query, update, UserActivityDocument.class);
    }

    /**
     * 최근 목록에 항목을 넣는다. 같은 이벤트가 다시 반영돼도 결과가 같다.
     *
     * <ul>
     *   <li>이미 같은 id 항목이 있으면 넣지 않는다 (필터의 $ne)
     *   <li>도착 순서가 아니라 createdAt 최신순으로 정렬한 뒤 10개만 남긴다. 늦게 다시 온 오래된 항목은 정렬 후 잘려 나간다
     * </ul>
     */
    private void pushRecent(UUID userId, String field, String idField, UUID id, Object item) {
        Query query = notContaining(userId, field, idField, id);
        Update update =
                new Update().push(field).sort(LATEST_FIRST).slice(RECENT_LIMIT).each(item);
        mongoTemplate.updateFirst(query, update, UserActivityDocument.class);
    }

    private Query notContaining(UUID userId, String field, String idField, UUID id) {
        return Query.query(
                Criteria.where("_id").is(userId).and(field + "." + idField).ne(id));
    }

    private Query byUser(UUID userId) {
        return Query.query(Criteria.where("_id").is(userId));
    }
}

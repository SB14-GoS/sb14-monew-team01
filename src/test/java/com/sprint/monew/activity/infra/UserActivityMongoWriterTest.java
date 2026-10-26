package com.sprint.monew.activity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.support.IntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

@IntegrationTest
class UserActivityMongoWriterTest {

    @Autowired
    UserActivityWriter writer;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    MongoTemplate mongoTemplate;

    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
    }

    @Test
    @DisplayName("문서를 만들면 사용자 정보와 빈 목록 4개로 조회된다")
    void create() {
        Instant createdAt = Instant.parse("2026-10-26T01:00:00Z");

        writer.create(userId, "user@monew.com", "닉네임", createdAt);

        UserActivityDocument document = find(userId);
        assertThat(document.getEmail()).isEqualTo("user@monew.com");
        assertThat(document.getNickname()).isEqualTo("닉네임");
        assertThat(document.getCreatedAt()).isEqualTo(createdAt);
        assertThat(document.getSubscriptions()).isEmpty();
        assertThat(document.getComments()).isEmpty();
        assertThat(document.getCommentLikes()).isEmpty();
        assertThat(document.getArticleViews()).isEmpty();
    }

    @Test
    @DisplayName("같은 사용자로 다시 만들면 기존 문서를 덮어쓰지 않고 예외가 난다")
    void createDuplicate() {
        createUser(userId);
        writer.addComment(userId, comment(UUID.randomUUID(), userId));

        assertThatThrownBy(() -> writer.create(userId, "other@monew.com", "다른닉네임", Instant.now()))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(find(userId).getComments()).hasSize(1);
    }

    @Test
    @DisplayName("닉네임을 바꾸면 닉네임만 바뀐다")
    void updateNickname() {
        createUser(userId);

        writer.updateNickname(userId, "새닉네임");

        UserActivityDocument document = find(userId);
        assertThat(document.getNickname()).isEqualTo("새닉네임");
        assertThat(document.getEmail()).isEqualTo("user@monew.com");
    }

    @Test
    @DisplayName("삭제하면 문서가 없어진다")
    void delete() {
        createUser(userId);

        writer.delete(userId);

        assertThat(repository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("댓글을 11개 추가하면 최신 10개만 최신순으로 남는다")
    void addCommentKeepsRecentTen() {
        createUser(userId);
        List<UUID> commentIds = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            UUID commentId = UUID.randomUUID();
            commentIds.add(commentId);
            writer.addComment(userId, comment(commentId, userId));
        }

        List<UUID> saved =
                find(userId).getComments().stream().map(CommentItem::commentId).toList();
        assertThat(saved).hasSize(10);
        assertThat(saved.get(0)).isEqualTo(commentIds.get(10));
        assertThat(saved).doesNotContain(commentIds.get(0));
    }

    @Test
    @DisplayName("문서가 없는 사용자에게 추가하면 문서를 만들지 않는다")
    void addToMissingUserDoesNotUpsert() {
        writer.addComment(userId, comment(UUID.randomUUID(), userId));

        assertThat(repository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("본 기사와 좋아요도 맨 앞에 넣고 10개만 남긴다")
    void addArticleViewAndCommentLikeKeepRecentTen() {
        createUser(userId);
        UUID lastArticleViewId = null;
        UUID lastCommentLikeId = null;
        for (int i = 0; i < 11; i++) {
            lastArticleViewId = UUID.randomUUID();
            lastCommentLikeId = UUID.randomUUID();
            writer.addArticleView(userId, articleView(lastArticleViewId));
            writer.addCommentLike(userId, commentLike(lastCommentLikeId, UUID.randomUUID()));
        }

        UserActivityDocument document = find(userId);
        assertThat(document.getArticleViews()).hasSize(10);
        assertThat(document.getArticleViews().get(0).articleViewId()).isEqualTo(lastArticleViewId);
        assertThat(document.getCommentLikes()).hasSize(10);
        assertThat(document.getCommentLikes().get(0).commentLikeId()).isEqualTo(lastCommentLikeId);
    }

    @Test
    @DisplayName("댓글 내용을 고치면 해당 댓글만 바뀐다")
    void updateCommentContent() {
        createUser(userId);
        UUID target = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        writer.addComment(userId, comment(target, userId));
        writer.addComment(userId, comment(other, userId));

        writer.updateCommentContent(userId, target, "수정된 내용");

        assertThat(commentById(userId, target).content()).isEqualTo("수정된 내용");
        assertThat(commentById(userId, other).content()).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("댓글을 지우면 해당 댓글만 빠지고 순서는 유지된다")
    void removeComment() {
        createUser(userId);
        UUID first = UUID.randomUUID();
        UUID middle = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        writer.addComment(userId, comment(first, userId));
        writer.addComment(userId, comment(middle, userId));
        writer.addComment(userId, comment(last, userId));

        writer.removeComment(userId, middle);

        assertThat(find(userId).getComments())
                .extracting(CommentItem::commentId)
                .containsExactly(last, first);
    }

    @Test
    @DisplayName("구독은 개수 제한 없이 모두 남고 최신이 맨 앞이다")
    void addSubscriptionKeepsAll() {
        createUser(userId);
        UUID lastInterestId = null;
        for (int i = 0; i < 11; i++) {
            lastInterestId = UUID.randomUUID();
            writer.addSubscription(userId, subscription(lastInterestId));
        }

        List<SubscriptionItem> subscriptions = find(userId).getSubscriptions();
        assertThat(subscriptions).hasSize(11);
        assertThat(subscriptions.get(0).interestId()).isEqualTo(lastInterestId);
    }

    @Test
    @DisplayName("구독을 취소하면 해당 구독만 빠지고 문서는 남는다")
    void removeSubscription() {
        createUser(userId);
        UUID kept = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        writer.addSubscription(userId, subscription(kept));
        writer.addSubscription(userId, subscription(removed));

        writer.removeSubscription(userId, removed);

        assertThat(find(userId).getSubscriptions())
                .extracting(SubscriptionItem::interestId)
                .containsExactly(kept);
    }

    @Test
    @DisplayName("관심사를 삭제하면 그 관심사를 구독한 모든 문서에서 빠진다")
    void removeInterestFromAll() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID userC = UUID.randomUUID();
        UUID deletedInterest = UUID.randomUUID();
        UUID otherInterest = UUID.randomUUID();
        createUser(userA);
        createUser(userB);
        createUser(userC);
        writer.addSubscription(userA, subscription(deletedInterest));
        writer.addSubscription(userB, subscription(deletedInterest));
        writer.addSubscription(userB, subscription(otherInterest));
        writer.addSubscription(userC, subscription(otherInterest));

        writer.removeInterestFromAll(deletedInterest);

        assertThat(find(userA).getSubscriptions()).isEmpty();
        assertThat(find(userB).getSubscriptions())
                .extracting(SubscriptionItem::interestId)
                .containsExactly(otherInterest);
        assertThat(find(userC).getSubscriptions())
                .extracting(SubscriptionItem::interestId)
                .containsExactly(otherInterest);
    }

    @Test
    @DisplayName("좋아요를 취소하면 해당 좋아요만 빠진다")
    void removeCommentLike() {
        createUser(userId);
        UUID keptCommentId = UUID.randomUUID();
        UUID removedCommentId = UUID.randomUUID();
        writer.addCommentLike(userId, commentLike(UUID.randomUUID(), keptCommentId));
        writer.addCommentLike(userId, commentLike(UUID.randomUUID(), removedCommentId));

        writer.removeCommentLike(userId, removedCommentId);

        assertThat(find(userId).getCommentLikes())
                .extracting(CommentLikeItem::commentId)
                .containsExactly(keptCommentId);
    }

    @Test
    @DisplayName("댓글 좋아요 수를 바꾸면 작성자 문서의 해당 댓글만 바뀐다")
    void updateCommentLikeCount() {
        createUser(userId);
        UUID target = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        writer.addComment(userId, comment(target, userId));
        writer.addComment(userId, comment(other, userId));

        writer.updateCommentLikeCount(userId, target, 5L);

        assertThat(commentById(userId, target).likeCount()).isEqualTo(5L);
        assertThat(commentById(userId, other).likeCount()).isZero();
    }

    private void createUser(UUID id) {
        writer.create(id, "user@monew.com", "닉네임", Instant.parse("2026-10-26T00:00:00Z"));
    }

    private UserActivityDocument find(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    private CommentItem commentById(UUID id, UUID commentId) {
        return find(id).getComments().stream()
                .filter(item -> item.commentId().equals(commentId))
                .findFirst()
                .orElseThrow();
    }

    private CommentItem comment(UUID commentId, UUID authorId) {
        return new CommentItem(commentId, UUID.randomUUID(), "기사 제목", authorId, "닉네임", "댓글 내용", 0L, Instant.now());
    }

    private CommentLikeItem commentLike(UUID commentLikeId, UUID commentId) {
        return new CommentLikeItem(
                commentLikeId,
                Instant.now(),
                commentId,
                UUID.randomUUID(),
                "기사 제목",
                UUID.randomUUID(),
                "작성자",
                "댓글 내용",
                1L,
                Instant.now());
    }

    private SubscriptionItem subscription(UUID interestId) {
        return new SubscriptionItem(UUID.randomUUID(), interestId, "관심사", List.of("키워드"), 1L, Instant.now());
    }

    private ArticleViewItem articleView(UUID articleViewId) {
        return new ArticleViewItem(
                articleViewId,
                userId,
                Instant.now(),
                UUID.randomUUID(),
                "NAVER",
                "https://news.example.com/1",
                "기사 제목",
                Instant.now(),
                "요약",
                0L,
                1L);
    }
}

package com.sprint.monew.activity.infra;

import static org.assertj.core.api.Assertions.assertThat;

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

    /** 항목마다 1초씩 늦은 시각. 최신순 정렬이 Instant.now()의 밀리초 충돌에 흔들리지 않게 한다. */
    private Instant clock = Instant.parse("2026-10-26T00:00:00Z");

    private Instant nextTime() {
        clock = clock.plusSeconds(1);
        return clock;
    }

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
    @DisplayName("같은 사용자로 다시 만들어도 예외 없이 기존 문서를 그대로 둔다")
    void createDuplicate() {
        createUser(userId);
        writer.addComment(userId, comment(UUID.randomUUID(), userId));

        writer.create(userId, "other@monew.com", "다른닉네임", Instant.now());

        UserActivityDocument document = find(userId);
        assertThat(document.getEmail()).isEqualTo("user@monew.com");
        assertThat(document.getNickname()).isEqualTo("닉네임");
        assertThat(document.getComments()).hasSize(1);
        assertThat(mongoTemplate.getCollection("user_activities").countDocuments())
                .isEqualTo(1);
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
    @DisplayName("삭제된 댓글의 좋아요는 모든 사용자 문서에서 빠지고 다른 좋아요는 남는다")
    void removeCommentLikeFromAll() {
        UUID otherUserId = UUID.randomUUID();
        createUser(userId);
        createUser(otherUserId);
        UUID deletedCommentId = UUID.randomUUID();
        UUID keptCommentId = UUID.randomUUID();
        writer.addCommentLike(userId, commentLike(UUID.randomUUID(), deletedCommentId));
        writer.addCommentLike(userId, commentLike(UUID.randomUUID(), keptCommentId));
        writer.addCommentLike(otherUserId, commentLike(UUID.randomUUID(), deletedCommentId));

        writer.removeCommentLikeFromAll(deletedCommentId);

        assertThat(find(userId).getCommentLikes())
                .extracting(CommentLikeItem::commentId)
                .containsExactly(keptCommentId);
        assertThat(find(otherUserId).getCommentLikes()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 기사는 모든 사용자의 본 기사 목록에서 빠지고 다른 기사는 남는다")
    void removeArticleViewFromAll() {
        UUID otherUserId = UUID.randomUUID();
        createUser(userId);
        createUser(otherUserId);
        ArticleViewItem kept = articleView(UUID.randomUUID());
        ArticleViewItem deleted = articleView(UUID.randomUUID());
        writer.addArticleView(userId, kept);
        writer.addArticleView(userId, deleted);
        writer.addArticleView(otherUserId, deleted);

        writer.removeArticleViewFromAll(deleted.articleId());

        assertThat(find(userId).getArticleViews())
                .extracting(ArticleViewItem::articleId)
                .containsExactly(kept.articleId());
        assertThat(find(otherUserId).getArticleViews()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 사용자가 쓴 댓글의 좋아요만 모든 사용자 문서에서 빠진다")
    void removeCommentLikesOfUserFromAll() {
        UUID otherUserId = UUID.randomUUID();
        createUser(userId);
        createUser(otherUserId);
        CommentLikeItem deletedUsersComment = commentLike(UUID.randomUUID(), UUID.randomUUID());
        CommentLikeItem keptLike = commentLike(UUID.randomUUID(), UUID.randomUUID());
        writer.addCommentLike(userId, deletedUsersComment);
        writer.addCommentLike(userId, keptLike);
        writer.addCommentLike(otherUserId, deletedUsersComment);

        writer.removeCommentLikesOfUserFromAll(deletedUsersComment.commentUserId());

        assertThat(find(userId).getCommentLikes())
                .extracting(CommentLikeItem::commentId)
                .containsExactly(keptLike.commentId());
        assertThat(find(otherUserId).getCommentLikes()).isEmpty();
    }

    @Test
    @DisplayName("댓글 좋아요 수를 바꾸면 작성자 문서의 해당 댓글만 바뀐다")
    void updateCommentLikeCount() {
        createUser(userId);
        UUID target = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        writer.addComment(userId, comment(target, userId));
        writer.addComment(userId, comment(other, userId));

        writer.updateCommentLikeCount(userId, target, 5L, 1L);

        assertThat(commentById(userId, target).likeCount()).isEqualTo(5L);
        assertThat(commentById(userId, other).likeCount()).isZero();
    }

    // ========================================================================================
    // 같은 이벤트가 다시 반영되거나 순서가 뒤바뀌어도 결과가 같다 (#198)

    @Test
    @DisplayName("같은 항목을 두 번 넣어도 한 번만 남는다")
    void addTwiceKeepsOne() {
        createUser(userId);
        CommentItem comment = comment(UUID.randomUUID(), userId);
        ArticleViewItem view = articleView(UUID.randomUUID());
        CommentLikeItem like = commentLike(UUID.randomUUID(), UUID.randomUUID());
        SubscriptionItem subscription = subscription(UUID.randomUUID());

        for (int i = 0; i < 2; i++) {
            writer.addComment(userId, comment);
            writer.addArticleView(userId, view);
            writer.addCommentLike(userId, like);
            writer.addSubscription(userId, subscription);
        }

        UserActivityDocument document = find(userId);
        assertThat(document.getComments()).hasSize(1);
        assertThat(document.getArticleViews()).hasSize(1);
        assertThat(document.getCommentLikes()).hasSize(1);
        assertThat(document.getSubscriptions()).hasSize(1);
    }

    @Test
    @DisplayName("늦게 도착한 항목도 createdAt 최신순 자리에 들어간다")
    void addOutOfOrderSortsByCreatedAt() {
        createUser(userId);
        CommentItem older = comment(UUID.randomUUID(), userId);
        CommentItem newer = comment(UUID.randomUUID(), userId);

        writer.addComment(userId, newer);
        writer.addComment(userId, older);

        assertThat(find(userId).getComments())
                .extracting(CommentItem::commentId)
                .containsExactly(newer.commentId(), older.commentId());
    }

    @Test
    @DisplayName("최신 10개에서 밀려난 항목이 다시 와도 되살아나지 않는다")
    void staleItemDoesNotComeBack() {
        createUser(userId);
        CommentItem stale = comment(UUID.randomUUID(), userId);
        writer.addComment(userId, stale);
        for (int i = 0; i < 10; i++) {
            writer.addComment(userId, comment(UUID.randomUUID(), userId));
        }

        writer.addComment(userId, stale);

        assertThat(find(userId).getComments())
                .hasSize(10)
                .extracting(CommentItem::commentId)
                .doesNotContain(stale.commentId());
    }

    @Test
    @DisplayName("좋아요 수는 저장된 버전보다 새 이벤트일 때만 바뀐다")
    void likeCountIgnoresOlderVersion() {
        createUser(userId);
        UUID target = UUID.randomUUID();
        writer.addComment(userId, comment(target, userId));

        writer.updateCommentLikeCount(userId, target, 6L, 11L);
        writer.updateCommentLikeCount(userId, target, 5L, 10L); // 늦게 도착한 이전 이벤트
        writer.updateCommentLikeCount(userId, target, 9L, 11L); // 같은 버전 재반영

        assertThat(commentById(userId, target).likeCount()).isEqualTo(6L);

        writer.updateCommentLikeCount(userId, target, 7L, 12L);

        assertThat(commentById(userId, target).likeCount()).isEqualTo(7L);
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
        return new CommentItem(commentId, UUID.randomUUID(), "기사 제목", authorId, "닉네임", "댓글 내용", 0L, nextTime());
    }

    private CommentLikeItem commentLike(UUID commentLikeId, UUID commentId) {
        return new CommentLikeItem(
                commentLikeId,
                nextTime(),
                commentId,
                UUID.randomUUID(),
                "기사 제목",
                UUID.randomUUID(),
                "작성자",
                "댓글 내용",
                1L,
                nextTime());
    }

    private SubscriptionItem subscription(UUID interestId) {
        return new SubscriptionItem(UUID.randomUUID(), interestId, "관심사", List.of("키워드"), 1L, nextTime());
    }

    private ArticleViewItem articleView(UUID articleViewId) {
        return new ArticleViewItem(
                articleViewId,
                userId,
                nextTime(),
                UUID.randomUUID(),
                "NAVER",
                "https://news.example.com/1",
                "기사 제목",
                nextTime(),
                "요약",
                0L,
                1L);
    }
}

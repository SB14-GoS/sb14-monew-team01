package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.activity.application.UserActivityReconciler.ReconcileResult;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UserActivityReconcilerTest {

    private static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired
    UserActivityReconciler reconciler;

    @Autowired
    UserActivityWriter writer;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JobOperator jobOperator;

    @Autowired
    Job userActivityReconcileJob;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> articleIds = new ArrayList<>();
    private final List<UUID> interestIds = new ArrayList<>();

    private UUID me;
    private UUID author;
    private UUID article;
    private UUID interest;
    private UUID keptComment;
    private UUID missingComment;
    private UUID deletedComment;
    private UUID authorComment;
    private UUID likeId;
    private UUID viewId;
    private UUID subscriptionId;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
        me = insertUser("me");
        author = insertUser("author");
        article = insertArticle();
        interest = insertInterest();

        keptComment = insertComment(me, 1, false);
        missingComment = insertComment(me, 2, false);
        deletedComment = insertComment(me, 3, true);
        authorComment = insertComment(author, 4, false);
        likeId = insertLike(me, authorComment, 5);
        viewId = insertView(me, 6);
        subscriptionId = insertSubscription(me, 7);
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
        for (UUID userId : userIds) {
            jdbcTemplate.update("delete from comment_likes where user_id = ?", userId);
            jdbcTemplate.update("delete from article_views where user_id = ?", userId);
            jdbcTemplate.update("delete from subscriptions where user_id = ?", userId);
        }
        for (UUID userId : userIds) {
            jdbcTemplate.update("delete from comments where user_id = ?", userId);
        }
        for (UUID userId : userIds) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        articleIds.forEach(id -> jdbcTemplate.update("delete from articles where id = ?", id));
        for (UUID id : interestIds) {
            jdbcTemplate.update("delete from interest_keywords where interest_id = ?", id);
            jdbcTemplate.update("delete from interests where id = ?", id);
        }
    }

    @Test
    @DisplayName("빠진 항목은 넣고 남은 항목은 지우며, 이미 있는 항목의 스냅샷 값은 그대로 둔다")
    void repairsDocument() {
        writer.create(me, "me@monew.com", "옛닉네임", T0);
        writer.addComment(me, commentItem(keptComment, 99L, 1)); // 스냅샷 좋아요 수 99
        writer.addComment(me, commentItem(deletedComment, 0L, 3)); // 원본에서 논리 삭제됨
        writer.addCommentLike(me, likeItem(UUID.randomUUID(), UUID.randomUUID())); // 원본에 없는 좋아요
        writer.addArticleView(me, viewItem(UUID.randomUUID())); // 원본에 없는 본 기사

        ReconcileResult result = reconciler.reconcile(List.of(me, author));

        UserActivityDocument document = repository.findById(me).orElseThrow();
        assertThat(document.getNickname()).isEqualTo("me-" + suffix);
        assertThat(document.getComments())
                .extracting(CommentItem::commentId)
                .containsExactly(missingComment, keptComment);
        assertThat(document.getComments())
                .filteredOn(item -> item.commentId().equals(keptComment))
                .singleElement()
                .extracting(CommentItem::likeCount)
                .isEqualTo(99L);
        assertThat(document.getCommentLikes()).singleElement().satisfies(like -> {
            assertThat(like.commentLikeId()).isEqualTo(likeId);
            assertThat(like.commentId()).isEqualTo(authorComment);
            assertThat(like.commentUserNickname()).isEqualTo("author-" + suffix);
        });
        assertThat(document.getArticleViews())
                .extracting(ArticleViewItem::articleViewId)
                .containsExactly(viewId);
        assertThat(document.getSubscriptions()).singleElement().satisfies(subscription -> {
            assertThat(subscription.subscriptionId()).isEqualTo(subscriptionId);
            assertThat(subscription.interestKeywords()).containsExactly("키워드");
        });

        assertThat(repository.findById(author).orElseThrow().getComments())
                .extracting(CommentItem::commentId)
                .containsExactly(authorComment);
        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.repaired()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1); // author 문서
        assertThat(result.itemsRemoved()).isEqualTo(3); // 삭제된 댓글, 없는 좋아요, 없는 본 기사
        assertThat(result.itemsAdded()).isEqualTo(5); // me: 댓글·좋아요·본 기사·구독, author: 댓글
    }

    @Test
    @DisplayName("다시 돌려도 더 고칠 것이 없다")
    void idempotent() {
        reconciler.reconcile(List.of(me, author));

        ReconcileResult second = reconciler.reconcile(List.of(me, author));

        assertThat(second.repaired()).isZero();
        assertThat(second.itemsAdded()).isZero();
        assertThat(second.itemsRemoved()).isZero();
    }

    @Test
    @DisplayName("같은 관심사·댓글에 이전 항목이 남아 있으면 원본 항목으로 바꾼다 (취소 이벤트 유실 후 재구독·재좋아요)")
    void replacesStaleItemWithSameKey() {
        writer.create(me, "me@monew.com", "me-" + suffix, T0);
        UUID staleSubscriptionId = UUID.randomUUID();
        writer.addSubscription(me, new SubscriptionItem(staleSubscriptionId, interest, "관심사", List.of(), 1L, T0));
        // 같은 댓글에 이전 좋아요와 원본 좋아요가 둘 다 남은 상태
        mongoTemplate
                .getCollection("user_activities")
                .updateOne(
                        new Document("_id", me),
                        new Document(
                                "$push",
                                new Document(
                                        "commentLikes",
                                        new Document(
                                                "$each",
                                                List.of(likeDocument(UUID.randomUUID()), likeDocument(likeId))))));

        reconciler.reconcile(List.of(me));

        UserActivityDocument document = repository.findById(me).orElseThrow();
        assertThat(document.getSubscriptions())
                .extracting(SubscriptionItem::subscriptionId)
                .containsExactly(subscriptionId);
        assertThat(document.getCommentLikes())
                .extracting(CommentLikeItem::commentLikeId)
                .containsExactly(likeId);
        assertThat(reconciler.reconcile(List.of(me)).repaired()).isZero();
    }

    @Test
    @DisplayName("원본에 11개가 있고 시각이 같아도 MongoDB와 같은 최근 10개를 고른다")
    void recentTenBoundaryWithSameCreatedAt() {
        jdbcTemplate.update("delete from comments where user_id = ?", me);
        Timestamp sameTime = at(30);
        for (int i = 0; i < 11; i++) {
            jdbcTemplate.update(
                    "insert into comments (id, article_id, user_id, content, like_count, created_at, updated_at) values (?, ?, ?, ?, 0, ?, ?)",
                    UUID.randomUUID(),
                    article,
                    me,
                    "같은 시각 " + i,
                    sameTime,
                    sameTime);
        }
        List<UUID> expected = jdbcTemplate.queryForList(
                "select id from comments where user_id = ? order by id desc limit 10", UUID.class, me);

        reconciler.reconcile(List.of(me));

        assertThat(repository.findById(me).orElseThrow().getComments())
                .extracting(CommentItem::commentId)
                .containsExactlyElementsOf(expected);
        assertThat(reconciler.reconcile(List.of(me)).repaired()).isZero();
    }

    @Test
    @DisplayName("배치는 전체 사용자를 보정하고 사용자가 사라진 문서를 지운다")
    void jobReconcilesAllAndRemovesOrphans() throws Exception {
        UUID orphan = UUID.randomUUID();
        writer.create(orphan, "orphan@monew.com", "고아", T0);

        JobExecution execution = jobOperator.start(
                userActivityReconcileJob,
                new JobParametersBuilder()
                        .addLocalDate(UserActivityReconcileJobConfig.TARGET_DATE, LocalDate.now())
                        .addString("run", suffix)
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(repository.findById(orphan)).isEmpty();
        assertThat(repository.findById(me).orElseThrow().getComments()).hasSize(2);
        assertThat(repository.findById(author)).isPresent();
    }

    // ========================================================================================
    // 원본(PostgreSQL) 데이터

    private UUID insertUser(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, email, nickname, password, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                id,
                name + "-" + suffix + "@monew.com",
                name + "-" + suffix,
                "password",
                at(0),
                at(0));
        userIds.add(id);
        return id;
    }

    private UUID insertArticle() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into articles (id, title, published_date, summary, source, source_url, comment_count, view_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 0, 0, ?, ?)
                """, id, "기사", at(0), "요약", "NAVER", "https://news.example.com/reconcile/" + suffix, at(0), at(0));
        articleIds.add(id);
        return id;
    }

    private UUID insertInterest() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into interests (id, name, subscriber_count, created_at, updated_at) values (?, ?, 1, ?, ?)",
                id,
                "관심사-" + suffix,
                at(0),
                at(0));
        jdbcTemplate.update(
                "insert into interest_keywords (id, interest_id, keyword, created_at, updated_at) values (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                id,
                "키워드",
                at(0),
                at(0));
        interestIds.add(id);
        return id;
    }

    private UUID insertComment(UUID userId, int minute, boolean deleted) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into comments (id, article_id, user_id, content, like_count, created_at, updated_at, deleted_at) values (?, ?, ?, ?, 0, ?, ?, ?)",
                id,
                article,
                userId,
                "댓글 " + minute,
                at(minute),
                at(minute),
                deleted ? at(minute) : null);
        return id;
    }

    private UUID insertLike(UUID userId, UUID commentId, int minute) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into comment_likes (id, comment_id, user_id, article_id, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                id,
                commentId,
                userId,
                article,
                at(minute),
                at(minute));
        return id;
    }

    private UUID insertView(UUID userId, int minute) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into article_views (id, article_id, user_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                id,
                article,
                userId,
                at(minute),
                at(minute));
        return id;
    }

    private UUID insertSubscription(UUID userId, int minute) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into subscriptions (id, user_id, interest_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                id,
                userId,
                interest,
                at(minute),
                at(minute));
        return id;
    }

    private static Timestamp at(int minute) {
        return Timestamp.from(T0.plusSeconds(minute * 60L));
    }

    // ========================================================================================
    // 활동 내역(MongoDB) 항목

    private CommentItem commentItem(UUID commentId, long likeCount, int minute) {
        return new CommentItem(commentId, article, "기사", me, "옛닉네임", "댓글", likeCount, T0.plusSeconds(minute * 60L));
    }

    private CommentLikeItem likeItem(UUID commentLikeId, UUID commentId) {
        return new CommentLikeItem(commentLikeId, T0, commentId, article, "기사", author, "작성자", "댓글", 1L, T0);
    }

    private ArticleViewItem viewItem(UUID articleViewId) {
        return new ArticleViewItem(articleViewId, me, T0, article, "NAVER", "url", "기사", T0, "요약", 0L, 1L);
    }

    private Document likeDocument(UUID commentLikeId) {
        Date time = Date.from(T0.plusSeconds(300));
        return new Document("commentLikeId", commentLikeId)
                .append("createdAt", time)
                .append("commentId", authorComment)
                .append("articleId", article)
                .append("articleTitle", "기사")
                .append("commentUserId", author)
                .append("commentUserNickname", "작성자")
                .append("commentContent", "댓글")
                .append("commentLikeCount", 1L)
                .append("commentCreatedAt", time);
    }
}

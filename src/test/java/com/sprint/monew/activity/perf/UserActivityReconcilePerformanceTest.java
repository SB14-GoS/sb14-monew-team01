package com.sprint.monew.activity.perf;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.activity.application.UserActivityReconciler;
import com.sprint.monew.activity.application.UserActivityReconciler.ReconcileResult;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.infra.UserActivityIndexInitializer;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * #198 정합성 보정 배치 측정: 빈 MongoDB에서 전체 재구성, 일부 유실 보정, 변경 없음 세 경우의 소요 시간.
 *
 * <pre>{@code PERF=true ./gradlew test --tests '*UserActivityReconcilePerformanceTest'}</pre>
 *
 * 사용자 수는 {@code PERF_RECONCILE_USERS}(기본 10000). 결과는 build/reports/performance/activity-reconcile.md
 */
@Tag("performance")
@IntegrationTest
@EnabledIfEnvironmentVariable(named = "PERF", matches = "true")
class UserActivityReconcilePerformanceTest {

    private static final String REPORT = "activity-reconcile";
    private static final int RECENT = 10;
    private static final int SUBSCRIPTIONS = 5;
    private static final int INTERESTS = 100;
    private static final double BROKEN_RATIO = 0.1;
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    UserActivityReconciler reconciler;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    UserActivityIndexInitializer indexInitializer;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final Random random = new Random(42);
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> articleIds = new ArrayList<>();
    private final List<UUID> interestIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
        deleteIn("comment_likes", "user_id", userIds);
        deleteIn("article_views", "user_id", userIds);
        deleteIn("subscriptions", "user_id", userIds);
        deleteIn("comments", "user_id", userIds);
        deleteIn("users", "id", userIds);
        deleteIn("articles", "id", articleIds);
        deleteIn("interest_keywords", "interest_id", interestIds);
        deleteIn("interests", "id", interestIds);
    }

    @Test
    @DisplayName("전체 재구성·일부 유실 보정·변경 없음 소요 시간")
    void measure() {
        int users = Integer.parseInt(System.getenv().getOrDefault("PERF_RECONCILE_USERS", "10000"));
        long seedStart = System.nanoTime();
        seedPostgres(users);
        long seedMs = (System.nanoTime() - seedStart) / 1_000_000;
        mongoTemplate.dropCollection(UserActivityDocument.class);
        indexInitializer.ensureIndexes();

        Run rebuild = run();
        int broken = breakDocuments();
        Run repair = run();
        Run noop = run();

        assertThat(rebuild.result().created()).isGreaterThanOrEqualTo(users);
        assertThat(repair.result().repaired()).isEqualTo(broken);
        assertThat(noop.result().repaired()).isZero();

        PerfReport.write(REPORT, """

                ## 활동 내역 정합성 보정 배치 측정 - 사용자 %,d명 (%s)
                - 사용자당 구독 %d, 댓글·좋아요·본 기사 %d / 원본 시드 %,dms / 묶음 크기 %d (묶음당 원본 쿼리 6개)
                - 일부 유실: 문서 %,d개(%.0f%%)에서 댓글 1개를 지우고 없는 본 기사 1개를 넣음

                | 경우 | 소요(ms) | 사용자당(ms) | 검사 | 고친 문서 | 새 문서 | 넣은 항목 | 지운 항목 |
                | --- | --- | --- | --- | --- | --- | --- | --- |
                %s
                %s
                %s
                """.formatted(
                        users,
                        Instant.now(),
                        SUBSCRIPTIONS,
                        RECENT,
                        seedMs,
                        500,
                        broken,
                        BROKEN_RATIO * 100,
                        rebuild.toRow("빈 MongoDB에서 전체 재구성"),
                        repair.toRow("일부 유실 보정"),
                        noop.toRow("변경 없음")));
    }

    private Run run() {
        long start = System.nanoTime();
        ReconcileResult result = reconciler.reconcileAll();
        return new Run(result, (System.nanoTime() - start) / 1_000_000.0);
    }

    /** 일부 문서에서 댓글 하나를 빼고, 원본에 없는 본 기사를 하나 넣는다. */
    private int breakDocuments() {
        int broken = 0;
        for (UUID userId : userIds) {
            if (random.nextDouble() >= BROKEN_RATIO) {
                continue;
            }
            mongoTemplate
                    .getCollection("user_activities")
                    .updateOne(
                            new Document("_id", userId),
                            new Document("$pop", new Document("comments", -1))
                                    .append("$push", new Document("articleViews", fakeView(userId))));
            broken++;
        }
        return broken;
    }

    private static Document fakeView(UUID userId) {
        Date at = Date.from(BASE_TIME);
        return new Document("articleViewId", UUID.randomUUID())
                .append("viewedBy", userId)
                .append("createdAt", at)
                .append("articleId", UUID.randomUUID())
                .append("source", "NAVER")
                .append("sourceUrl", "https://news.example.com/fake")
                .append("articleTitle", "원본에 없는 기사")
                .append("articlePublishedDate", at)
                .append("articleSummary", "요약")
                .append("articleCommentCount", 0L)
                .append("articleViewCount", 0L);
    }

    // ========================================================================================
    // 원본(PostgreSQL) 데이터

    private void seedPostgres(int users) {
        Timestamp at = Timestamp.from(BASE_TIME);
        List<Object[]> interests = new ArrayList<>();
        List<Object[]> keywords = new ArrayList<>();
        for (int i = 0; i < INTERESTS; i++) {
            UUID id = UUID.randomUUID();
            interestIds.add(id);
            interests.add(new Object[] {id, "관심사-" + suffix + "-" + i, at, at});
            keywords.add(new Object[] {UUID.randomUUID(), id, "키워드" + i, at, at});
        }
        jdbcTemplate.batchUpdate(
                "insert into interests (id, name, subscriber_count, created_at, updated_at) values (?, ?, 1, ?, ?)",
                interests);
        jdbcTemplate.batchUpdate(
                "insert into interest_keywords (id, interest_id, keyword, created_at, updated_at) values (?, ?, ?, ?, ?)",
                keywords);

        List<Object[]> articles = new ArrayList<>();
        for (int i = 0; i < Math.max(100, users / 5); i++) {
            UUID id = UUID.randomUUID();
            articleIds.add(id);
            articles.add(new Object[] {
                id, "기사 " + i, at, "요약", "NAVER", "https://news.example.com/rc/" + suffix + "/" + i, at, at
            });
        }
        jdbcTemplate.batchUpdate("""
                insert into articles (id, title, published_date, summary, source, source_url, comment_count, view_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 0, 0, ?, ?)
                """, articles);

        List<Object[]> userRows = new ArrayList<>();
        for (int u = 0; u < users; u++) {
            UUID id = UUID.randomUUID();
            userIds.add(id);
            userRows.add(new Object[] {id, "rc-" + suffix + "-" + u + "@monew.com", "u" + u, "password", at, at});
        }
        batch(
                "insert into users (id, email, nickname, password, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                userRows);

        List<UUID> commentIds = new ArrayList<>(users * RECENT);
        List<Object[]> comments = new ArrayList<>();
        List<Object[]> views = new ArrayList<>();
        List<Object[]> subscriptions = new ArrayList<>();
        for (int u = 0; u < users; u++) {
            UUID userId = userIds.get(u);
            for (int r = 0; r < RECENT; r++) {
                Timestamp time = Timestamp.from(BASE_TIME.plusSeconds(r));
                UUID commentId = UUID.randomUUID();
                commentIds.add(commentId);
                comments.add(new Object[] {commentId, randomArticle(), userId, "댓글", time, time});
                views.add(new Object[] {
                    UUID.randomUUID(), articleIds.get((u * RECENT + r) % articleIds.size()), userId, time, time
                });
            }
            for (int s = 0; s < SUBSCRIPTIONS; s++) {
                subscriptions.add(
                        new Object[] {UUID.randomUUID(), userId, interestIds.get((u + s) % INTERESTS), at, at});
            }
        }
        batch(
                "insert into comments (id, article_id, user_id, content, like_count, created_at, updated_at) values (?, ?, ?, ?, 0, ?, ?)",
                comments);
        batch(
                "insert into article_views (id, article_id, user_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                views);
        batch(
                "insert into subscriptions (id, user_id, interest_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                subscriptions);

        // 다른 사용자의 댓글에 좋아요 (사용자당 10개, 같은 댓글에 두 번 누르지 않게 연속된 댓글을 고른다)
        List<Object[]> likes = new ArrayList<>();
        for (int u = 0; u < users; u++) {
            UUID likerId = userIds.get(u);
            int start = ((u + 1) % users) * RECENT;
            for (int r = 0; r < RECENT; r++) {
                Timestamp time = Timestamp.from(BASE_TIME.plusSeconds(r));
                likes.add(new Object[] {
                    UUID.randomUUID(), commentIds.get(start + r), likerId, articleIds.get(0), time, time
                });
            }
        }
        batch(
                "insert into comment_likes (id, comment_id, user_id, article_id, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                likes);
    }

    private UUID randomArticle() {
        return articleIds.get(random.nextInt(articleIds.size()));
    }

    private void batch(String sql, List<Object[]> rows) {
        for (int from = 0; from < rows.size(); from += 5_000) {
            jdbcTemplate.batchUpdate(sql, rows.subList(from, Math.min(rows.size(), from + 5_000)));
        }
    }

    private void deleteIn(String table, String column, List<UUID> ids) {
        for (int from = 0; from < ids.size(); from += 1_000) {
            List<UUID> chunk = ids.subList(from, Math.min(ids.size(), from + 1_000));
            String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
            jdbcTemplate.update(
                    "delete from " + table + " where " + column + " in (" + placeholders + ")", chunk.toArray());
        }
    }

    private record Run(ReconcileResult result, double elapsedMs) {

        String toRow(String label) {
            return "| %s | %,.0f | %.3f | %,d | %,d | %,d | %,d | %,d |"
                    .formatted(
                            label,
                            elapsedMs,
                            result.scanned() == 0 ? 0 : elapsedMs / result.scanned(),
                            result.scanned(),
                            result.repaired(),
                            result.created(),
                            result.itemsAdded(),
                            result.itemsRemoved());
        }
    }
}

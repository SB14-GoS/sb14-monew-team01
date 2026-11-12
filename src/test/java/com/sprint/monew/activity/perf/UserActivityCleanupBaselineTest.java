package com.sprint.monew.activity.perf;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoCollection;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.activity.infra.UserActivityIndexInitializer;
import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * #198 기준 측정: user_activities 전체 정리 쿼리(updateMulti)와 그 쿼리를 요청 스레드에서 실행하는 댓글 삭제 API의 지연 시간.
 *
 * <p>무거운 테스트라 {@code PERF=true}일 때만 실행한다. 사용자 수는 {@code PERF_USERS}(쉼표 구분, 기본 10000,100000)로 바꾼다.
 *
 * <pre>{@code PERF=true ./gradlew test --tests '*UserActivityCleanupBaselineTest'}</pre>
 *
 * 결과는 콘솔과 build/reports/performance/activity-cleanup.md 에 남는다.
 */
@Tag("performance")
@IntegrationTest
@EnabledIfEnvironmentVariable(named = "PERF", matches = "true")
class UserActivityCleanupBaselineTest {

    private static final String REPORT = "activity-cleanup";
    private static final String COLLECTION = "user_activities";

    private static final int WARM_UP = 3;
    private static final int ITERATIONS = 20;
    private static final int TARGETS = WARM_UP + ITERATIONS;

    private static final int SUBSCRIPTIONS_PER_USER = 5;
    private static final int RECENT_PER_USER = 10;
    private static final int INTEREST_POOL = 100;
    /** 댓글 하나에 좋아요한 문서 수 평균 */
    private static final int LIKERS_PER_COMMENT = 20;
    /** 기사 하나를 본 문서 수 평균 */
    private static final int VIEWERS_PER_ARTICLE = 50;

    private static final int BATCH_SIZE = 2_000;

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    UserActivityWriter writer;

    @Autowired
    UserActivityIndexInitializer indexInitializer;

    @Autowired
    CommentService commentService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final List<UUID> postgresUserIds = new ArrayList<>();
    private UUID postgresArticleId;

    static Stream<Integer> userCounts() {
        String value = System.getenv().getOrDefault("PERF_USERS", "10000,100000");
        return Arrays.stream(value.split(",")).map(String::trim).map(Integer::parseInt);
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
        if (postgresArticleId != null) {
            jdbcTemplate.update("delete from comments where article_id = ?", postgresArticleId);
            jdbcTemplate.update("delete from articles where id = ?", postgresArticleId);
        }
        for (UUID userId : postgresUserIds) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        postgresUserIds.clear();
        postgresArticleId = null;
    }

    @ParameterizedTest(name = "사용자 {0}명")
    @MethodSource("userCounts")
    @DisplayName("전체 정리 쿼리 실행 계획·지연 시간과 댓글 삭제 API 지연 시간")
    void measure(int users) {
        mongoTemplate.dropCollection(UserActivityDocument.class);
        indexInitializer.ensureIndexes(); // 운영 기동 시와 같게 인덱스가 있는 상태에서 데이터를 쌓는다
        long seedStart = System.nanoTime();
        Seed seed = seedMongo(users);
        long seedMs = (System.nanoTime() - seedStart) / 1_000_000;
        assertThat(mongoTemplate.getCollection(COLLECTION).countDocuments()).isEqualTo(users);

        StringBuilder report = new StringBuilder();
        report.append("\n## user_activities 정리 쿼리 측정 - 사용자 %,d명 (%s)\n".formatted(users, Instant.now()));
        report.append("- 문서당 구독 %d, 댓글 %d, 좋아요 %d, 본 기사 %d / 시드 %,dms / 인덱스: %s\n"
                .formatted(
                        SUBSCRIPTIONS_PER_USER,
                        RECENT_PER_USER,
                        RECENT_PER_USER,
                        RECENT_PER_USER,
                        seedMs,
                        indexNames()));

        report.append("\n### 실행 계획 (find + explain executionStats)\n");
        report.append("| 필터 | 계획 | nReturned | keysExamined | docsExamined | 실행(ms) |\n");
        report.append("| --- | --- | --- | --- | --- | --- |\n");
        report.append(explainRow("commentLikes.commentId", seed.likeTargets.get(0)));
        report.append(explainRow("articleViews.articleId", seed.viewTargets.get(0)));
        report.append(explainRow("subscriptions.interestId", seed.interestTargets.get(0)));
        report.append(explainRow("commentLikes.commentUserId", seed.authorTargets.get(0)));

        report.append("\n### 정리 쿼리 지연 시간 (UserActivityWriter, 워밍업 %d회 후 %d회)\n".formatted(WARM_UP, ITERATIONS));
        report.append(LatencyStats.header()).append('\n');
        report.append(measureEach("removeCommentLikeFromAll", seed.likeTargets, writer::removeCommentLikeFromAll));
        report.append(measureEach("removeArticleViewFromAll", seed.viewTargets, writer::removeArticleViewFromAll));
        report.append(measureEach("removeInterestFromAll", seed.interestTargets, writer::removeInterestFromAll));
        report.append(measureEach(
                "removeCommentLikesOfUserFromAll", seed.authorTargets, writer::removeCommentLikesOfUserFromAll));

        seedPostgresComments(seed);
        report.append("\n### 댓글 삭제 API 지연 시간 (CommentService.delete, AFTER_COMMIT 리스너 포함)\n");
        report.append(LatencyStats.header()).append('\n');
        LatencyStats delete =
                LatencyStats.measure(WARM_UP, ITERATIONS, i -> commentService.delete(seed.deleteTargets.get(i), null));
        report.append(delete.toRow("CommentService.delete")).append('\n');

        PerfReport.write(REPORT, report.toString());
    }

    // ========================================================================================
    // 측정

    private String explainRow(String field, UUID value) {
        Document explain = mongoTemplate
                .getCollection(COLLECTION)
                .find(new Document(field, value))
                .explain(ExplainVerbosity.EXECUTION_STATS);
        Document stats = explain.get("executionStats", Document.class);
        Document winningPlan = explain.get("queryPlanner", Document.class).get("winningPlan", Document.class);
        if (winningPlan.containsKey("queryPlan")) {
            winningPlan = winningPlan.get("queryPlan", Document.class);
        }
        return "| %s | %s | %d | %d | %d | %d |\n"
                .formatted(
                        field,
                        stageChain(winningPlan),
                        number(stats, "nReturned"),
                        number(stats, "totalKeysExamined"),
                        number(stats, "totalDocsExamined"),
                        number(stats, "executionTimeMillis"));
    }

    private String measureEach(String label, List<UUID> targets, Consumer<UUID> operation) {
        LatencyStats stats = LatencyStats.measure(WARM_UP, ITERATIONS, i -> operation.accept(targets.get(i)));
        return stats.toRow(label) + '\n';
    }

    private static String stageChain(Document plan) {
        List<String> stages = new ArrayList<>();
        Document node = plan;
        while (node != null) {
            stages.add(node.getString("stage"));
            Object input = node.get("inputStage");
            if (input == null && node.get("inputStages") instanceof List<?> inputs && !inputs.isEmpty()) {
                input = inputs.get(0);
            }
            node = input instanceof Document document ? document : null;
        }
        return String.join(" > ", stages);
    }

    private static long number(Document document, String key) {
        Object value = document.get(key);
        return value instanceof Number number ? number.longValue() : -1;
    }

    private String indexNames() {
        List<String> names = new ArrayList<>();
        mongoTemplate.getCollection(COLLECTION).listIndexes().forEach(index -> names.add(index.getString("name")));
        return String.join(", ", names);
    }

    // ========================================================================================
    // MongoDB 데이터 준비 (Projector가 만드는 문서와 같은 모양)

    private Seed seedMongo(int users) {
        Random random = new Random(42);
        UUID[] userIds = randomIds(users);
        UUID[] interestIds = randomIds(INTEREST_POOL);
        UUID[] articleIds = randomIds(Math.max(TARGETS * 2, users * RECENT_PER_USER / VIEWERS_PER_ARTICLE));
        UUID[] likedCommentIds = randomIds(Math.max(TARGETS * 4, users * RECENT_PER_USER / LIKERS_PER_COMMENT));
        UUID[] likedCommentAuthors = new UUID[likedCommentIds.length];
        for (int i = 0; i < likedCommentIds.length; i++) {
            likedCommentAuthors[i] = userIds[random.nextInt(users)];
        }

        MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);
        List<Document> batch = new ArrayList<>(BATCH_SIZE);
        Date at = Date.from(BASE_TIME);
        for (int u = 0; u < users; u++) {
            List<Document> subscriptions = new ArrayList<>();
            for (int s = 0; s < SUBSCRIPTIONS_PER_USER; s++) {
                UUID interestId = interestIds[random.nextInt(INTEREST_POOL)];
                subscriptions.add(new Document("subscriptionId", UUID.randomUUID())
                        .append("interestId", interestId)
                        .append("interestName", "관심사")
                        .append("interestKeywords", List.of("키워드1", "키워드2"))
                        .append("interestSubscriberCount", 10L)
                        .append("createdAt", at));
            }
            List<Document> comments = new ArrayList<>();
            List<Document> commentLikes = new ArrayList<>();
            List<Document> articleViews = new ArrayList<>();
            for (int r = 0; r < RECENT_PER_USER; r++) {
                UUID articleId = articleIds[random.nextInt(articleIds.length)];
                comments.add(new Document("commentId", UUID.randomUUID())
                        .append("articleId", articleId)
                        .append("articleTitle", "기사 제목")
                        .append("userId", userIds[u])
                        .append("userNickname", "닉네임")
                        .append("content", "댓글 내용")
                        .append("likeCount", 0L)
                        .append("createdAt", at));

                int liked = random.nextInt(likedCommentIds.length);
                commentLikes.add(new Document("commentLikeId", UUID.randomUUID())
                        .append("createdAt", at)
                        .append("commentId", likedCommentIds[liked])
                        .append("articleId", articleId)
                        .append("articleTitle", "기사 제목")
                        .append("commentUserId", likedCommentAuthors[liked])
                        .append("commentUserNickname", "작성자")
                        .append("commentContent", "댓글 내용")
                        .append("commentLikeCount", 1L)
                        .append("commentCreatedAt", at));

                articleViews.add(new Document("articleViewId", UUID.randomUUID())
                        .append("viewedBy", userIds[u])
                        .append("createdAt", at)
                        .append("articleId", articleIds[random.nextInt(articleIds.length)])
                        .append("source", "NAVER")
                        .append("sourceUrl", "https://news.example.com/article")
                        .append("articleTitle", "기사 제목")
                        .append("articlePublishedDate", at)
                        .append("articleSummary", "기사 요약")
                        .append("articleCommentCount", 0L)
                        .append("articleViewCount", 1L));
            }
            batch.add(new Document("_id", userIds[u])
                    .append("email", "user" + u + "@monew.com")
                    .append("nickname", "user" + u)
                    .append("createdAt", at)
                    .append("subscriptions", subscriptions)
                    .append("comments", comments)
                    .append("commentLikes", commentLikes)
                    .append("articleViews", articleViews)
                    .append("_class", UserActivityDocument.class.getName()));
            if (batch.size() == BATCH_SIZE) {
                collection.insertMany(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            collection.insertMany(batch);
        }

        // 측정 대상은 서로 겹치지 않게 나눈다 (한 번 지운 대상은 다시 쓰지 않는다)
        List<UUID> likeTargets = List.of(Arrays.copyOfRange(likedCommentIds, 0, TARGETS));
        List<UUID> deleteTargets = List.of(Arrays.copyOfRange(likedCommentIds, TARGETS, TARGETS * 2));
        List<UUID> deleteAuthors = List.of(Arrays.copyOfRange(likedCommentAuthors, TARGETS, TARGETS * 2));
        Set<UUID> authors = new LinkedHashSet<>(
                Arrays.asList(Arrays.copyOfRange(likedCommentAuthors, TARGETS * 2, likedCommentAuthors.length)));
        authors.removeAll(deleteAuthors);
        List<UUID> authorTargets = authors.stream().limit(TARGETS).toList();
        List<UUID> viewTargets = List.of(Arrays.copyOfRange(articleIds, 0, TARGETS));
        List<UUID> interestTargets = List.of(Arrays.copyOfRange(interestIds, 0, TARGETS));
        return new Seed(likeTargets, viewTargets, interestTargets, authorTargets, deleteTargets, deleteAuthors);
    }

    private static UUID[] randomIds(int count) {
        UUID[] ids = new UUID[count];
        for (int i = 0; i < count; i++) {
            ids[i] = UUID.randomUUID();
        }
        return ids;
    }

    // ========================================================================================
    // PostgreSQL 데이터 준비 (삭제할 댓글과 작성자, 기사)

    private void seedPostgresComments(Seed seed) {
        Timestamp at = Timestamp.from(BASE_TIME);
        postgresArticleId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into articles (id, title, published_date, summary, source, source_url, comment_count, view_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                postgresArticleId,
                "기사",
                at,
                "요약",
                "NAVER",
                "https://news.example.com/perf/" + suffix,
                TARGETS,
                at,
                at);

        for (UUID authorId : new LinkedHashSet<>(seed.deleteAuthors)) {
            jdbcTemplate.update(
                    "insert into users (id, email, nickname, password, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                    authorId,
                    "perf-" + suffix + "-" + postgresUserIds.size() + "@monew.com",
                    "작성자",
                    "password",
                    at,
                    at);
            postgresUserIds.add(authorId);
        }

        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < seed.deleteTargets.size(); i++) {
            rows.add(new Object[] {seed.deleteTargets.get(i), postgresArticleId, seed.deleteAuthors.get(i), "댓글", at, at
            });
        }
        jdbcTemplate.batchUpdate(
                "insert into comments (id, article_id, user_id, content, like_count, created_at, updated_at) values (?, ?, ?, ?, 0, ?, ?)",
                rows);
    }

    private record Seed(
            List<UUID> likeTargets,
            List<UUID> viewTargets,
            List<UUID> interestTargets,
            List<UUID> authorTargets,
            List<UUID> deleteTargets,
            List<UUID> deleteAuthors) {}
}

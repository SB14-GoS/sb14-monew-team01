package com.sprint.monew.activity;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * #44 완료 조건: 활동 내역을 PostgreSQL 조인으로 조회할 때와 MongoDB 문서 1건으로 조회할 때의 시간을 비교한다.
 *
 * <p>환경에 따라 수치가 흔들리므로 속도는 assert하지 않고 출력만 한다. 두 방식이 같은 건수를 돌려주는지만 검증한다.
 */
@Tag("performance")
@IntegrationTest
@Transactional
class UserActivityPerformanceComparisonTest {

    private static final int WARM_UP = 20;
    private static final int ITERATIONS = 100;
    private static final int SUBSCRIPTIONS = 5;
    private static final int RECENT_LIMIT = 10;
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private static final String USER_SQL = "select id, email, nickname, created_at from users where id = ?";

    private static final String SUBSCRIPTION_SQL = """
            select s.id, i.id as interest_id, i.name, i.subscriber_count, s.created_at, k.keyword
            from subscriptions s
            join interests i on i.id = s.interest_id
            left join interest_keywords k on k.interest_id = i.id
            where s.user_id = ?
            order by s.created_at desc
            """;

    private static final String COMMENT_SQL = """
            select c.id, c.article_id, a.title, c.content, c.like_count, c.created_at
            from comments c
            join articles a on a.id = c.article_id
            where c.user_id = ? and c.deleted_at is null
            order by c.created_at desc
            limit 10
            """;

    private static final String COMMENT_LIKE_SQL = """
            select cl.id, cl.created_at, c.id as comment_id, c.content, c.like_count, c.created_at as comment_created_at,
                   u.id as comment_user_id, u.nickname, a.id as article_id, a.title
            from comment_likes cl
            join comments c on c.id = cl.comment_id
            join users u on u.id = c.user_id
            join articles a on a.id = c.article_id
            where cl.user_id = ?
            order by cl.created_at desc
            limit 10
            """;

    private static final String ARTICLE_VIEW_SQL = """
            select v.id, v.created_at, a.id as article_id, a.source, a.source_url, a.title, a.published_date,
                   a.summary, a.comment_count, a.view_count
            from article_views v
            join articles a on a.id = v.article_id
            where v.user_id = ?
            order by v.created_at desc
            limit 10
            """;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserActivityWriter writer;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    MongoTemplate mongoTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
    }

    @ParameterizedTest(name = "{0}: 댓글 {1}, 본 기사 {2}, 좋아요 {3}")
    @CsvSource({"명세 크기, 10, 10, 10", "대량, 1000, 1000, 200"})
    @DisplayName("활동 내역 조회: PostgreSQL 조인 5회 vs MongoDB 문서 1건")
    void compare(String scenario, int comments, int views, int likes) {
        UUID userId = UUID.randomUUID();
        seedPostgres(userId, comments, views, likes);
        seedMongo(userId);

        JoinResult joinResult = joinQuery(userId);
        UserActivityDocument document = repository.findById(userId).orElseThrow();
        assertThat(joinResult.subscriptions())
                .isEqualTo(document.getSubscriptions().size());
        assertThat(joinResult.comments()).isEqualTo(document.getComments().size());
        assertThat(joinResult.commentLikes())
                .isEqualTo(document.getCommentLikes().size());
        assertThat(joinResult.articleViews())
                .isEqualTo(document.getArticleViews().size());

        Stats join = measure(() -> joinQuery(userId));
        Stats mongo = measure(() -> repository.findById(userId).orElseThrow());

        System.out.printf(
                """

                [활동 내역 조회 성능 비교 - %s] 댓글 %d, 본 기사 %d, 좋아요 %d, 구독 %d (워밍업 %d회 후 %d회 측정)
                | 방식 | 쿼리 수 | 평균(ms) | p95(ms) |
                | --- | --- | --- | --- |
                | PostgreSQL 조인 | 5 | %.3f | %.3f |
                | MongoDB 문서 1건 | 1 | %.3f | %.3f |
                %n""",
                scenario,
                comments,
                views,
                likes,
                SUBSCRIPTIONS,
                WARM_UP,
                ITERATIONS,
                join.averageMs(),
                join.p95Ms(),
                mongo.averageMs(),
                mongo.p95Ms());
    }

    private JoinResult joinQuery(UUID userId) {
        jdbcTemplate.queryForMap(USER_SQL, userId);
        List<Map<String, Object>> subscriptionRows = jdbcTemplate.queryForList(SUBSCRIPTION_SQL, userId);
        List<Map<String, Object>> commentRows = jdbcTemplate.queryForList(COMMENT_SQL, userId);
        List<Map<String, Object>> commentLikeRows = jdbcTemplate.queryForList(COMMENT_LIKE_SQL, userId);
        List<Map<String, Object>> articleViewRows = jdbcTemplate.queryForList(ARTICLE_VIEW_SQL, userId);

        // 키워드 조인으로 구독 행이 늘어나므로 구독 ID 기준으로 센다
        int subscriptions = (int)
                subscriptionRows.stream().map(row -> row.get("id")).distinct().count();
        return new JoinResult(subscriptions, commentRows.size(), commentLikeRows.size(), articleViewRows.size());
    }

    private Stats measure(Supplier<?> query) {
        for (int i = 0; i < WARM_UP; i++) {
            query.get();
        }
        long[] elapsed = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            query.get();
            elapsed[i] = System.nanoTime() - start;
        }
        Arrays.sort(elapsed);
        double average = Arrays.stream(elapsed).average().orElse(0) / 1_000_000.0;
        double p95 = elapsed[(int) Math.ceil(ITERATIONS * 0.95) - 1] / 1_000_000.0;
        return new Stats(average, p95);
    }

    // ========================================================================================
    // PostgreSQL 데이터 준비 (조인 방식이 읽을 원본 테이블)

    private void seedPostgres(UUID userId, int comments, int views, int likes) {
        UUID otherUserId = UUID.randomUUID();
        insertUser(userId, "me");
        insertUser(otherUserId, "other");

        for (int i = 0; i < SUBSCRIPTIONS; i++) {
            UUID interestId = UUID.randomUUID();
            Timestamp at = at(i);
            jdbcTemplate.update(
                    "insert into interests (id, name, subscriber_count, created_at, updated_at) values (?, ?, 1, ?, ?)",
                    interestId,
                    "관심사-" + suffix + "-" + i,
                    at,
                    at);
            for (int k = 0; k < 2; k++) {
                jdbcTemplate.update(
                        "insert into interest_keywords (id, interest_id, keyword, created_at, updated_at) values (?, ?, ?, ?, ?)",
                        UUID.randomUUID(),
                        interestId,
                        "키워드" + k,
                        at,
                        at);
            }
            jdbcTemplate.update(
                    "insert into subscriptions (id, user_id, interest_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    userId,
                    interestId,
                    at,
                    at);
        }

        List<UUID> articleIds = insertArticles(Math.max(views, RECENT_LIMIT));

        List<Object[]> myComments = new ArrayList<>();
        for (int i = 0; i < comments; i++) {
            Timestamp at = at(i);
            myComments.add(
                    new Object[] {UUID.randomUUID(), articleIds.get(i % articleIds.size()), userId, "내 댓글 " + i, at, at
                    });
        }
        jdbcTemplate.batchUpdate(
                "insert into comments (id, article_id, user_id, content, like_count, created_at, updated_at) values (?, ?, ?, ?, 0, ?, ?)",
                myComments);

        List<Object[]> otherComments = new ArrayList<>();
        List<Object[]> commentLikes = new ArrayList<>();
        for (int i = 0; i < likes; i++) {
            UUID commentId = UUID.randomUUID();
            UUID articleId = articleIds.get(i % articleIds.size());
            Timestamp at = at(i);
            otherComments.add(new Object[] {commentId, articleId, otherUserId, "남의 댓글 " + i, at, at});
            commentLikes.add(new Object[] {UUID.randomUUID(), commentId, userId, articleId, at, at});
        }
        jdbcTemplate.batchUpdate(
                "insert into comments (id, article_id, user_id, content, like_count, created_at, updated_at) values (?, ?, ?, ?, 1, ?, ?)",
                otherComments);
        jdbcTemplate.batchUpdate(
                "insert into comment_likes (id, comment_id, user_id, article_id, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                commentLikes);

        List<Object[]> articleViews = new ArrayList<>();
        for (int i = 0; i < views; i++) {
            Timestamp at = at(i);
            articleViews.add(new Object[] {UUID.randomUUID(), articleIds.get(i), userId, at, at});
        }
        jdbcTemplate.batchUpdate(
                "insert into article_views (id, article_id, user_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                articleViews);
    }

    private void insertUser(UUID id, String name) {
        Timestamp at = at(0);
        jdbcTemplate.update(
                "insert into users (id, email, nickname, password, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                id,
                name + "-" + suffix + "@monew.com",
                name,
                "password",
                at,
                at);
    }

    private List<UUID> insertArticles(int count) {
        List<UUID> ids = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID id = UUID.randomUUID();
            Timestamp at = at(i);
            ids.add(id);
            rows.add(new Object[] {
                id, "기사 " + i, at, "요약 " + i, "NAVER", "https://news.example.com/" + suffix + "/" + i, at, at
            });
        }
        jdbcTemplate.batchUpdate("""
                insert into articles (id, title, published_date, summary, source, source_url, comment_count, view_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 0, 1, ?, ?)
                """, rows);
        return ids;
    }

    // ========================================================================================
    // MongoDB 데이터 준비 (활동 시점에 미리 만들어 둔 문서와 같은 모양)

    private void seedMongo(UUID userId) {
        writer.create(userId, "me-" + suffix + "@monew.com", "me", BASE_TIME);
        for (int i = 0; i < SUBSCRIPTIONS; i++) {
            writer.addSubscription(
                    userId,
                    new SubscriptionItem(
                            UUID.randomUUID(), UUID.randomUUID(), "관심사-" + i, List.of("키워드0", "키워드1"), 1L, BASE_TIME));
        }
        for (int i = 0; i < RECENT_LIMIT; i++) {
            writer.addComment(
                    userId,
                    new CommentItem(
                            UUID.randomUUID(), UUID.randomUUID(), "기사 " + i, userId, "me", "내 댓글 " + i, 0L, BASE_TIME));
            writer.addCommentLike(
                    userId,
                    new CommentLikeItem(
                            UUID.randomUUID(),
                            BASE_TIME,
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "기사 " + i,
                            UUID.randomUUID(),
                            "other",
                            "남의 댓글 " + i,
                            1L,
                            BASE_TIME));
            writer.addArticleView(
                    userId,
                    new ArticleViewItem(
                            UUID.randomUUID(),
                            userId,
                            BASE_TIME,
                            UUID.randomUUID(),
                            "NAVER",
                            "https://news.example.com/" + i,
                            "기사 " + i,
                            BASE_TIME,
                            "요약 " + i,
                            0L,
                            1L));
        }
    }

    private Timestamp at(int seconds) {
        return Timestamp.from(BASE_TIME.plusSeconds(seconds));
    }

    private record JoinResult(int subscriptions, int comments, int commentLikes, int articleViews) {}

    private record Stats(double averageMs, double p95Ms) {}
}

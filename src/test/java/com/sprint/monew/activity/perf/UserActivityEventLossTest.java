package com.sprint.monew.activity.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.sprint.monew.activity.application.ActivityOutboxRelay;
import com.sprint.monew.activity.domain.ActivityOutbox;
import com.sprint.monew.activity.domain.ActivityOutboxRepository;
import com.sprint.monew.activity.domain.ActivityOutboxStatus;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.article.application.ArticleViewService;
import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.support.IntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * #198: MongoDB 장애 중에 일어난 활동이 활동 내역에서 사라지지 않는지 센다.
 *
 * <p>Writer를 spy로 감싸 장애 구간 동안 MongoDB 예외를 던지게 한다. 원본(PostgreSQL)은 커밋되므로, 원본과 활동 내역 문서를 비교해 빠진 항목을 유실로 센다.
 * 장애 구간에도 relay를 몇 번 돌려(스케줄러 실행 흉내) 재시도가 쌓이게 하고, 복구 후 relay가 outbox를 모두 비우면 유실이 0이어야 한다.
 * 재시도 대기 시간은 0으로 둬 복구 직후 바로 반영되게 한다.
 *
 * <pre>{@code PERF=true ./gradlew test --tests '*UserActivityEventLossTest'}</pre>
 */
@Tag("performance")
@IntegrationTest
@EnabledIfEnvironmentVariable(named = "PERF", matches = "true")
@TestPropertySource(properties = "monew.activity.outbox.base-backoff=0ms")
class UserActivityEventLossTest {

    private static final String REPORT = "activity-event-loss";
    private static final int USERS = 100;
    private static final int OUTAGE_TICKS = 5;
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @MockitoSpyBean
    UserActivityWriter writer;

    @Autowired
    CommentService commentService;

    @Autowired
    ArticleViewService articleViewService;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    ActivityOutboxRelay relay;

    @Autowired
    ActivityOutboxRepository outboxRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> articleIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        reset(writer);
        outboxRepository.deleteAll();
        mongoTemplate.dropCollection(UserActivityDocument.class);
        for (UUID userId : userIds) {
            jdbcTemplate.update("delete from comments where user_id = ?", userId);
            jdbcTemplate.update("delete from article_views where user_id = ?", userId);
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        for (UUID articleId : articleIds) {
            jdbcTemplate.update("delete from articles where id = ?", articleId);
        }
    }

    @Test
    @DisplayName("MongoDB 장애 구간의 활동도 복구 후 relay가 모두 반영한다")
    void measureLossDuringOutage() {
        outboxRepository.deleteAll();
        UUID outageArticle = insertArticle(0);
        UUID recoveredArticle = insertArticle(1);
        for (int i = 0; i < USERS; i++) {
            UUID userId = insertUser(i);
            writer.create(userId, "loss-" + suffix + "-" + i + "@monew.com", "user" + i, BASE_TIME);
        }

        // 장애 구간: MongoDB 쓰기가 모두 실패한다
        DataAccessResourceFailureException outage = new DataAccessResourceFailureException("simulated mongo outage");
        doThrow(outage).when(writer).addComment(any(), any());
        doThrow(outage).when(writer).addArticleView(any(), any());
        int apiFailures = runActivities(outageArticle);
        int blockedTicks = 0;
        for (int i = 0; i < OUTAGE_TICKS; i++) {
            if (relay.relayBatch().blocked()) {
                blockedTicks++;
            }
        }
        long pendingAfterOutage = outboxRepository.countByStatus(ActivityOutboxStatus.PENDING);
        int headAttempts = outboxRepository.findAll().stream()
                .mapToInt(ActivityOutbox::getAttempts)
                .max()
                .orElse(0);

        // 복구 후: 같은 활동을 한 번 더 하고 relay로 outbox를 비운다
        reset(writer);
        apiFailures += runActivities(recoveredArticle);
        long pendingBeforeDrain = outboxRepository.countByStatus(ActivityOutboxStatus.PENDING);
        long drainStart = System.nanoTime();
        int relayed = drain();
        double drainMs = (System.nanoTime() - drainStart) / 1_000_000.0;

        Result comments = compare(
                "select id from comments where user_id = ? and deleted_at is null",
                document -> document.getComments().stream()
                        .map(UserActivityDocument.CommentItem::commentId)
                        .toList());
        Result views = compare(
                "select id from article_views where user_id = ?",
                document -> document.getArticleViews().stream()
                        .map(UserActivityDocument.ArticleViewItem::articleViewId)
                        .toList());

        assertThat(comments.source()).isEqualTo(USERS * 2);
        assertThat(views.source()).isEqualTo(USERS * 2);
        assertThat(outboxRepository.countByStatus(ActivityOutboxStatus.PENDING)).isZero();
        assertThat(outboxRepository.countByStatus(ActivityOutboxStatus.DEAD)).isZero();
        assertThat(comments.lost()).isZero();
        assertThat(views.lost()).isZero();

        PerfReport.write(REPORT, """

                ## MongoDB 장애 중 활동 내역 유실 측정 - Transactional Outbox (%s)
                - 사용자 %d명이 장애 구간과 복구 후에 각각 댓글 1건 작성 + 기사 1건 조회
                - API 실패: %d건
                - 장애 중 relay %d회 실행, 막힘 %d회 / 맨 앞 이벤트 시도 %d회 / 장애 직후 PENDING %d건
                - 복구 후 PENDING %d건 → relay가 %d건 반영 (%.1fms, 건당 %.3fms)

                | 항목 | 원본(PostgreSQL) | 활동 내역(MongoDB) | 유실 | 유실률 |
                | --- | --- | --- | --- | --- |
                %s
                %s
                """.formatted(
                        Instant.now(),
                        USERS,
                        apiFailures,
                        OUTAGE_TICKS,
                        blockedTicks,
                        headAttempts,
                        pendingAfterOutage,
                        pendingBeforeDrain,
                        relayed,
                        drainMs,
                        relayed == 0 ? 0 : drainMs / relayed,
                        comments.toRow("댓글"),
                        views.toRow("본 기사")));
    }

    private int drain() {
        int relayed = 0;
        ActivityOutboxRelay.RelayResult result;
        do {
            result = relay.relayBatch();
            relayed += result.done();
        } while (result.hasMore() || (!result.blocked() && result.done() > 0));
        return relayed;
    }

    private int runActivities(UUID articleId) {
        int failures = 0;
        for (UUID userId : userIds) {
            try {
                commentService.register(articleId, userId, "댓글 " + suffix);
                articleViewService.register(articleId, userId);
            } catch (RuntimeException exception) {
                failures++;
            }
        }
        return failures;
    }

    private Result compare(String sourceSql, Function<UserActivityDocument, List<UUID>> projected) {
        int source = 0;
        int lost = 0;
        for (UUID userId : userIds) {
            List<UUID> sourceIds = jdbcTemplate.queryForList(sourceSql, UUID.class, userId);
            Set<UUID> projectedIds =
                    new HashSet<>(projected.apply(repository.findById(userId).orElseThrow()));
            source += sourceIds.size();
            lost += (int)
                    sourceIds.stream().filter(id -> !projectedIds.contains(id)).count();
        }
        return new Result(source, lost);
    }

    private UUID insertUser(int index) {
        UUID id = UUID.randomUUID();
        Timestamp at = Timestamp.from(BASE_TIME);
        jdbcTemplate.update(
                "insert into users (id, email, nickname, password, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                id,
                "loss-" + suffix + "-" + index + "@monew.com",
                "user" + index,
                "password",
                at,
                at);
        userIds.add(id);
        return id;
    }

    private UUID insertArticle(int index) {
        UUID id = UUID.randomUUID();
        Timestamp at = Timestamp.from(BASE_TIME);
        jdbcTemplate.update(
                """
                insert into articles (id, title, published_date, summary, source, source_url, comment_count, view_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 0, 0, ?, ?)
                """,
                id,
                "기사 " + index,
                at,
                "요약",
                "NAVER",
                "https://news.example.com/loss/" + suffix + "/" + index,
                at,
                at);
        articleIds.add(id);
        return id;
    }

    private record Result(int source, int lost) {

        String toRow(String label) {
            int projected = source - lost;
            double rate = source == 0 ? 0 : lost * 100.0 / source;
            return "| %s | %d | %d | %d | %.1f%% |".formatted(label, source, projected, lost, rate);
        }
    }
}

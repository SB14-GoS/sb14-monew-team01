package com.sprint.monew.activity.infra;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivitySource;
import com.sprint.monew.activity.domain.UserActivitySourceReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 정합성 보정용 원본 조회. 사용자 묶음(chunk)마다 쿼리 6개로 읽는다 (사용자마다 조회하지 않는다).
 *
 * <p>여러 모듈의 테이블을 직접 읽는다. 다른 모듈의 API를 사용자마다 부르면 N+1이 되고 각 모듈에 보정 전용 API가 생기므로,
 * 읽기 전용 보정 쿼리에 한해 테이블 결합을 허용한다. 필터는 Projector의 반영 규칙과 같게 맞춘다
 * (논리 삭제된 댓글과 그 좋아요는 제외, 본 기사는 기사 물리 삭제 때만 빠짐).
 */
@Repository
@RequiredArgsConstructor
public class UserActivityJdbcSourceReader implements UserActivitySourceReader {

    private static final int RECENT_LIMIT = 10;

    private static final String USER_IDS_AFTER_SQL = "select id from users where id > :after order by id limit :limit";

    private static final String USERS_SQL = "select id, email, nickname, created_at from users where id in (:ids)";

    private static final String EXISTING_USERS_SQL = "select id from users where id in (:ids)";

    private static final String SUBSCRIPTIONS_SQL = """
            select s.id, s.user_id, s.created_at, i.id as interest_id, i.name, i.subscriber_count
            from subscriptions s
            join interests i on i.id = s.interest_id
            where s.user_id in (:ids)
            order by s.created_at desc, s.id desc
            """;

    private static final String KEYWORDS_SQL =
            "select interest_id, keyword from interest_keywords where interest_id in (:ids) order by created_at, id";

    private static final String COMMENTS_SQL = """
            select * from (
                select c.id, c.article_id, a.title, c.user_id, u.nickname, c.content, c.like_count, c.created_at,
                       row_number() over (partition by c.user_id order by c.created_at desc, c.id desc) as rn
                from comments c
                join articles a on a.id = c.article_id
                join users u on u.id = c.user_id
                where c.user_id in (:ids) and c.deleted_at is null
            ) recent
            where rn <= :limit
            order by user_id, rn
            """;

    private static final String COMMENT_LIKES_SQL = """
            select * from (
                select cl.id, cl.created_at, cl.user_id as liker_id, c.id as comment_id, c.article_id, a.title,
                       c.user_id as comment_user_id, u.nickname, c.content, c.like_count,
                       c.created_at as comment_created_at,
                       row_number() over (partition by cl.user_id order by cl.created_at desc, cl.id desc) as rn
                from comment_likes cl
                join comments c on c.id = cl.comment_id
                join users u on u.id = c.user_id
                join articles a on a.id = c.article_id
                where cl.user_id in (:ids) and c.deleted_at is null
            ) recent
            where rn <= :limit
            order by liker_id, rn
            """;

    private static final String ARTICLE_VIEWS_SQL = """
            select * from (
                select v.id, v.user_id, v.created_at, a.id as article_id, a.source, a.source_url, a.title,
                       a.published_date, a.summary, a.comment_count, a.view_count,
                       row_number() over (partition by v.user_id order by v.created_at desc, v.id desc) as rn
                from article_views v
                join articles a on a.id = v.article_id
                where v.user_id in (:ids)
            ) recent
            where rn <= :limit
            order by user_id, rn
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<UUID> findUserIdsAfter(UUID after, int limit) {
        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("after", after).addValue("limit", limit);
        return jdbcTemplate.queryForList(USER_IDS_AFTER_SQL, params, UUID.class);
    }

    @Override
    public Set<UUID> findExistingUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbcTemplate.queryForList(EXISTING_USERS_SQL, ids(userIds), UUID.class));
    }

    @Override
    public Map<UUID, UserActivitySource> load(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource params = ids(userIds).addValue("limit", RECENT_LIMIT);

        Map<UUID, Builder> builders = new LinkedHashMap<>();
        jdbcTemplate.query(USERS_SQL, params, (ResultSet rs) -> {
            UUID id = rs.getObject("id", UUID.class);
            builders.put(
                    id, new Builder(id, rs.getString("email"), rs.getString("nickname"), instant(rs, "created_at")));
        });

        List<SubscriptionRow> subscriptionRows = jdbcTemplate.query(
                SUBSCRIPTIONS_SQL,
                params,
                (rs, i) -> new SubscriptionRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("id", UUID.class),
                        rs.getObject("interest_id", UUID.class),
                        rs.getString("name"),
                        rs.getLong("subscriber_count"),
                        instant(rs, "created_at")));
        Map<UUID, List<String>> keywords = keywordsOf(subscriptionRows);
        for (SubscriptionRow row : subscriptionRows) {
            withBuilder(
                    builders,
                    row.userId(),
                    builder -> builder.subscriptions.add(new SubscriptionItem(
                            row.subscriptionId(),
                            row.interestId(),
                            row.name(),
                            keywords.getOrDefault(row.interestId(), List.of()),
                            row.subscriberCount(),
                            row.createdAt())));
        }

        jdbcTemplate.query(COMMENTS_SQL, params, (ResultSet rs) -> {
            UUID userId = rs.getObject("user_id", UUID.class);
            withBuilder(
                    builders,
                    userId,
                    builder -> builder.comments.add(new CommentItem(
                            uuid(rs, "id"),
                            uuid(rs, "article_id"),
                            string(rs, "title"),
                            userId,
                            string(rs, "nickname"),
                            string(rs, "content"),
                            number(rs, "like_count"),
                            instantOf(rs, "created_at"))));
        });

        jdbcTemplate.query(COMMENT_LIKES_SQL, params, (ResultSet rs) -> {
            UUID likerId = rs.getObject("liker_id", UUID.class);
            withBuilder(
                    builders,
                    likerId,
                    builder -> builder.commentLikes.add(new CommentLikeItem(
                            uuid(rs, "id"),
                            instantOf(rs, "created_at"),
                            uuid(rs, "comment_id"),
                            uuid(rs, "article_id"),
                            string(rs, "title"),
                            uuid(rs, "comment_user_id"),
                            string(rs, "nickname"),
                            string(rs, "content"),
                            number(rs, "like_count"),
                            instantOf(rs, "comment_created_at"))));
        });

        jdbcTemplate.query(ARTICLE_VIEWS_SQL, params, (ResultSet rs) -> {
            UUID userId = rs.getObject("user_id", UUID.class);
            withBuilder(
                    builders,
                    userId,
                    builder -> builder.articleViews.add(new ArticleViewItem(
                            uuid(rs, "id"),
                            userId,
                            instantOf(rs, "created_at"),
                            uuid(rs, "article_id"),
                            string(rs, "source"),
                            string(rs, "source_url"),
                            string(rs, "title"),
                            instantOf(rs, "published_date"),
                            string(rs, "summary"),
                            number(rs, "comment_count"),
                            number(rs, "view_count"))));
        });

        Map<UUID, UserActivitySource> sources = new HashMap<>();
        builders.forEach((id, builder) -> sources.put(id, builder.build()));
        return sources;
    }

    private Map<UUID, List<String>> keywordsOf(List<SubscriptionRow> rows) {
        Set<UUID> interestIds = new HashSet<>();
        rows.forEach(row -> interestIds.add(row.interestId()));
        Map<UUID, List<String>> keywords = new HashMap<>();
        if (interestIds.isEmpty()) {
            return keywords;
        }
        jdbcTemplate.query(KEYWORDS_SQL, ids(interestIds), (ResultSet rs) -> {
            keywords.computeIfAbsent(rs.getObject("interest_id", UUID.class), id -> new ArrayList<>())
                    .add(rs.getString("keyword"));
        });
        return keywords;
    }

    private static void withBuilder(Map<UUID, Builder> builders, UUID userId, Consumer<Builder> action) {
        Builder builder = builders.get(userId);
        if (builder != null) {
            action.accept(builder);
        }
    }

    private static MapSqlParameterSource ids(Collection<UUID> ids) {
        return new MapSqlParameterSource("ids", ids);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    // RowCallbackHandler 람다 안에서 쓰기 위해 SQLException을 감싼다
    private static Instant instantOf(ResultSet rs, String column) {
        try {
            return instant(rs, column);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static UUID uuid(ResultSet rs, String column) {
        try {
            return rs.getObject(column, UUID.class);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String string(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static long number(ResultSet rs, String column) {
        try {
            return rs.getLong(column);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record SubscriptionRow(
            UUID userId, UUID subscriptionId, UUID interestId, String name, long subscriberCount, Instant createdAt) {}

    private static final class Builder {

        private final UUID userId;
        private final String email;
        private final String nickname;
        private final Instant createdAt;
        private final List<SubscriptionItem> subscriptions = new ArrayList<>();
        private final List<CommentItem> comments = new ArrayList<>();
        private final List<CommentLikeItem> commentLikes = new ArrayList<>();
        private final List<ArticleViewItem> articleViews = new ArrayList<>();

        private Builder(UUID userId, String email, String nickname, Instant createdAt) {
            this.userId = userId;
            this.email = email;
            this.nickname = nickname;
            this.createdAt = createdAt;
        }

        private UserActivitySource build() {
            return new UserActivitySource(
                    userId, email, nickname, createdAt, subscriptions, comments, commentLikes, articleViews);
        }
    }
}

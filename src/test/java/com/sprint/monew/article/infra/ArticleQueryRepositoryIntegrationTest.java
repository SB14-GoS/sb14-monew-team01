package com.sprint.monew.article.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.application.ArticleService;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.ArticleOrderBy;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.article.domain.repository.ArticleSearchCursor;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleQueryRepositoryIntegrationTest {

    private static final Instant BASE = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    ArticleQueryRepository repository;

    @Autowired
    EntityManager em;

    @Autowired
    ArticleService articleService;

    @Test
    void keywordMatchesTitleOrSummaryAndExcludesDeletedArticles() {
        Article title = save("경제 뉴스", null, ArticleSource.NAVER, 0, 1);
        Article summary = save("다른 제목", "경제 소식", ArticleSource.HANKYUNG, 1, 2);
        save("스포츠", null, ArticleSource.CHOSUN, 2, 3);
        Article deleted = save("경제 삭제", "경제", ArticleSource.NAVER, 3, 4);
        deleted.softDelete();
        em.flush();

        ArticleSearchCondition condition = condition(
                "경제", null, null, null, null, ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC, null, null, 10);

        assertThat(repository.search(condition, null))
                .extracting(Article::getId)
                .containsExactly(summary.getId(), title.getId());
        assertThat(repository.countByCondition(condition)).isEqualTo(2);
        assertThat(repository.countByCondition(condition(
                        " ", null, null, null, null, ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC, null, null, 10)))
                .isEqualTo(3);
    }

    @Test
    void combinesAllFiltersAndDoesNotDuplicateArticlesWithMultipleInterests() {
        UUID interestId = UUID.randomUUID();
        Article naver = save("경제", "뉴스", ArticleSource.NAVER, 0, 1);
        Article hankyung = save("뉴스", "경제", ArticleSource.HANKYUNG, 2, 2);
        Article otherSource = save("경제", null, ArticleSource.CHOSUN, 1, 3);
        Article tooLate = save("경제", null, ArticleSource.NAVER, 3, 4);
        Article otherKeyword = save("스포츠", null, ArticleSource.NAVER, 1, 5);
        save("경제", null, ArticleSource.NAVER, 1, 6);
        for (Article value : List.of(naver, hankyung, otherSource, tooLate, otherKeyword)) {
            link(value, interestId);
        }
        link(naver, UUID.randomUUID());
        em.flush();

        ArticleSearchCondition condition = condition(
                "경제",
                interestId,
                List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG),
                local(BASE),
                local(BASE.plusSeconds(2 * 86400)),
                ArticleOrderBy.PUBLISH_DATE,
                SortDirection.ASC,
                null,
                null,
                10);

        assertThat(repository.search(condition, null))
                .extracting(Article::getId)
                .containsExactly(naver.getId(), hankyung.getId());
        assertThat(repository.countByCondition(condition)).isEqualTo(2);
        assertThat(repository.countByCondition(condition(
                        null,
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        ArticleOrderBy.PUBLISH_DATE,
                        SortDirection.ASC,
                        null,
                        null,
                        10)))
                .isZero();
    }

    @Test
    void supportsOneSidedInclusiveDateRangesAndEmptySourceList() {
        Article first = save("첫 기사", null, ArticleSource.NAVER, 0, 1);
        Article second = save("다음 기사", null, ArticleSource.HANKYUNG, 1, 2);

        assertThat(repository.search(
                        condition(
                                null,
                                null,
                                List.of(),
                                local(BASE.plusSeconds(86400)),
                                null,
                                ArticleOrderBy.PUBLISH_DATE,
                                SortDirection.ASC,
                                null,
                                null,
                                10),
                        null))
                .extracting(Article::getId)
                .containsExactly(second.getId());
        assertThat(repository.search(
                        condition(
                                null,
                                null,
                                null,
                                null,
                                local(BASE),
                                ArticleOrderBy.PUBLISH_DATE,
                                SortDirection.ASC,
                                null,
                                null,
                                10),
                        null))
                .extracting(Article::getId)
                .containsExactly(first.getId());
    }

    @Test
    void filtersKoreanCalendarDayAtUtcBoundaries() {
        saveAt(Instant.parse("2026-10-21T14:59:59Z")); // 한국 시간 10월 21일 23:59:59
        Article first = saveAt(Instant.parse("2026-10-21T15:00:00Z")); // 한국 시간 10월 22일 00:00:00
        Article last = saveAt(Instant.parse("2026-10-22T14:59:59Z")); // 한국 시간 10월 22일 23:59:59
        saveAt(Instant.parse("2026-10-22T15:00:00Z")); // 한국 시간 10월 23일 00:00:00
        ArticleSearchCondition condition = condition(
                null,
                null,
                null,
                LocalDateTime.parse("2026-10-22T00:00:00"),
                LocalDateTime.parse("2026-10-22T23:59:59"),
                ArticleOrderBy.PUBLISH_DATE,
                SortDirection.ASC,
                null,
                null,
                10);

        assertThat(repository.search(condition, null))
                .extracting(Article::getId)
                .containsExactly(first.getId(), last.getId());
        assertThat(repository.countByCondition(condition)).isEqualTo(2);
    }

    static Stream<Arguments> sortOrders() {
        return Stream.of(ArticleOrderBy.PUBLISH_DATE, ArticleOrderBy.COMMENT_COUNT, ArticleOrderBy.VIEW_COUNT)
                .flatMap(order -> Stream.of(SortDirection.ASC, SortDirection.DESC)
                        .map(direction -> Arguments.of(order, direction)));
    }

    @ParameterizedTest
    @MethodSource("sortOrders")
    void paginatesEverySortInBothDirectionsWithoutCountingTheCursor(ArticleOrderBy orderBy, SortDirection direction) {
        Article high = save("높음", null, ArticleSource.NAVER, 3, 4);
        Article middleEarlier = save("중간 이전", null, ArticleSource.NAVER, 2, 2);
        Article middleLater = save("중간 이후", null, ArticleSource.NAVER, 2, 3);
        Article low = save("낮음", null, ArticleSource.NAVER, 1, 1);
        List<UUID> expected = direction == SortDirection.ASC
                ? List.of(low.getId(), middleEarlier.getId(), middleLater.getId(), high.getId())
                : List.of(high.getId(), middleLater.getId(), middleEarlier.getId(), low.getId());
        List<UUID> actual = new ArrayList<>();
        String cursor = null;
        Instant after = null;
        ArticleSearchCursor searchCursor = null;

        // Four rows with limit=1 should terminate in four pages, including a tied primary sort value.
        for (int page = 0; page < 5; page++) {
            ArticleSearchCondition condition =
                    condition(null, null, null, null, null, orderBy, direction, cursor, after, 1);
            List<Article> fetched = repository.search(condition, searchCursor);
            assertThat(fetched).hasSizeLessThanOrEqualTo(2);
            assertThat(repository.countByCondition(condition)).isEqualTo(4);
            if (fetched.isEmpty()) {
                break;
            }
            Article last = fetched.get(0);
            actual.add(last.getId());
            if (fetched.size() == 1) {
                break;
            }
            cursor = switch (orderBy) {
                case PUBLISH_DATE -> last.getPublishedDate().toString();
                case COMMENT_COUNT -> Long.toString(last.getCommentCount());
                case VIEW_COUNT -> Long.toString(last.getViewCount());
            };
            cursor += "|" + last.getId();
            after = last.getCreatedAt();
            searchCursor = switch (orderBy) {
                case PUBLISH_DATE -> new ArticleSearchCursor(last.getPublishedDate(), null, after, last.getId());
                case COMMENT_COUNT -> new ArticleSearchCursor(null, last.getCommentCount(), after, last.getId());
                case VIEW_COUNT -> new ArticleSearchCursor(null, last.getViewCount(), after, last.getId());
            };
        }
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @ParameterizedTest
    @MethodSource("sortOrders")
    void paginatesTiedSortValuesAndCreatedAtWithoutMissingArticles(ArticleOrderBy orderBy, SortDirection direction) {
        List<UUID> ids = List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"),
                UUID.fromString("80000000-0000-0000-0000-000000000000"),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        for (UUID id : ids) {
            Article value = save("동률 기사", null, ArticleSource.NAVER, 0, 1);
            em.createNativeQuery("update articles set id = :id where id = :oldId")
                    .setParameter("id", id)
                    .setParameter("oldId", value.getId())
                    .executeUpdate();
            em.clear();
        }
        List<UUID> expected = new ArrayList<>(ids);
        if (direction == SortDirection.DESC) {
            Collections.reverse(expected);
        }
        UUID userId = UUID.randomUUID();
        for (int limit : List.of(1, 2)) {
            List<UUID> actual = new ArrayList<>();
            String cursor = null;
            Instant after = null;
            for (int page = 0; page < ids.size(); page++) {
                var result = articleService.search(
                        condition(null, null, null, null, null, orderBy, direction, cursor, after, limit), userId);
                assertThat(result.totalElements()).isEqualTo(ids.size());
                actual.addAll(result.content().stream().map(dto -> dto.id()).toList());
                if (!result.hasNext()) {
                    assertThat(result.nextCursor()).isNull();
                    assertThat(result.nextAfter()).isNull();
                    break;
                }
                cursor = result.nextCursor();
                after = result.nextAfter();
            }
            assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
        }
    }

    @Test
    void emptyResultHasZeroCount() {
        ArticleSearchCondition condition =
                condition(null, null, null, null, null, ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC, null, null, 2);
        assertThat(repository.search(condition, null)).isEmpty();
        assertThat(repository.countByCondition(condition)).isZero();
    }

    private Article save(String title, String summary, ArticleSource source, int days, int createdSecond) {
        Article value = Article.create(
                title, BASE.plusSeconds(days * 86400L), summary, source, "https://example.com/" + UUID.randomUUID());
        em.persist(value);
        em.flush();
        em.createQuery(
                        "update Article a set a.createdAt = :createdAt, a.commentCount = :count, a.viewCount = :count where a.id = :id")
                .setParameter("createdAt", BASE.plusSeconds(createdSecond))
                .setParameter("count", (long) days)
                .setParameter("id", value.getId())
                .executeUpdate();
        UUID id = value.getId();
        em.clear();
        return em.find(Article.class, id);
    }

    private Article saveAt(Instant publishedDate) {
        Article value = Article.create(
                "경제 뉴스", publishedDate, null, ArticleSource.NAVER, "https://example.com/" + UUID.randomUUID());
        em.persist(value);
        return value;
    }

    private void link(Article value, UUID interestId) {
        em.persist(ArticleInterest.create(em.getReference(Article.class, value.getId()), interestId));
    }

    private static LocalDateTime local(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
    }

    private static ArticleSearchCondition condition(
            String keyword,
            UUID interestId,
            List<ArticleSource> sources,
            LocalDateTime from,
            LocalDateTime to,
            ArticleOrderBy orderBy,
            SortDirection direction,
            String cursor,
            Instant after,
            int limit) {
        return new ArticleSearchCondition(
                keyword, interestId, sources, from, to, orderBy, direction, new CursorRequest(cursor, after, limit));
    }
}

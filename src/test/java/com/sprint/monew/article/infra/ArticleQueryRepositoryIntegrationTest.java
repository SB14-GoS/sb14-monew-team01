package com.sprint.monew.article.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

    @Test
    void keywordMatchesTitleOrSummaryAndExcludesDeletedArticles() {
        Article title = save("경제 뉴스", null, ArticleSource.NAVER, 0, 1);
        Article summary = save("다른 제목", "경제 소식", ArticleSource.HANKYUNG, 1, 2);
        save("스포츠", null, ArticleSource.CHOSUN, 2, 3);
        Article deleted = save("경제 삭제", "경제", ArticleSource.NAVER, 3, 4);
        deleted.softDelete();
        em.flush();

        ArticleSearchCondition condition =
                condition("경제", null, null, null, null, "publishDate", SortDirection.DESC, null, null, 10);

        assertThat(repository.search(condition))
                .extracting(Article::getId)
                .containsExactly(summary.getId(), title.getId());
        assertThat(repository.countByCondition(condition)).isEqualTo(2);
        assertThat(repository.countByCondition(
                        condition(" ", null, null, null, null, "publishDate", SortDirection.ASC, null, null, 10)))
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
                "publishDate",
                SortDirection.ASC,
                null,
                null,
                10);

        assertThat(repository.search(condition))
                .extracting(Article::getId)
                .containsExactly(naver.getId(), hankyung.getId());
        assertThat(repository.countByCondition(condition)).isEqualTo(2);
        assertThat(repository.countByCondition(condition(
                        null, UUID.randomUUID(), null, null, null, "publishDate", SortDirection.ASC, null, null, 10)))
                .isZero();
    }

    @Test
    void supportsOneSidedInclusiveDateRangesAndEmptySourceList() {
        Article first = save("첫 기사", null, ArticleSource.NAVER, 0, 1);
        Article second = save("다음 기사", null, ArticleSource.HANKYUNG, 1, 2);

        assertThat(repository.search(condition(
                        null,
                        null,
                        List.of(),
                        local(BASE.plusSeconds(86400)),
                        null,
                        "publishDate",
                        SortDirection.ASC,
                        null,
                        null,
                        10)))
                .extracting(Article::getId)
                .containsExactly(second.getId());
        assertThat(repository.search(condition(
                        null, null, null, null, local(BASE), "publishDate", SortDirection.ASC, null, null, 10)))
                .extracting(Article::getId)
                .containsExactly(first.getId());
    }

    static Stream<Arguments> sortOrders() {
        return Stream.of("publishDate", "commentCount", "viewCount")
                .flatMap(order -> Stream.of(SortDirection.ASC, SortDirection.DESC)
                        .map(direction -> Arguments.of(order, direction)));
    }

    @ParameterizedTest
    @MethodSource("sortOrders")
    void paginatesEverySortInBothDirectionsWithoutCountingTheCursor(String orderBy, SortDirection direction) {
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

        // Four rows with limit=1 should terminate in four pages, including a tied primary sort value.
        for (int page = 0; page < 5; page++) {
            ArticleSearchCondition condition =
                    condition(null, null, null, null, null, orderBy, direction, cursor, after, 1);
            List<Article> fetched = repository.search(condition);
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
                case "publishDate" -> last.getPublishedDate().toString();
                case "commentCount" -> Long.toString(last.getCommentCount());
                case "viewCount" -> Long.toString(last.getViewCount());
                default -> throw new AssertionError(orderBy);
            };
            after = last.getCreatedAt();
        }
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    static Stream<ArticleSearchCondition> invalidConditions() {
        return Stream.of(
                condition(null, null, null, null, null, "unknown", SortDirection.ASC, null, null, 2),
                condition(null, null, null, null, null, "publishDate", null, null, null, 2),
                condition(
                        null,
                        null,
                        null,
                        local(BASE.plusSeconds(1)),
                        local(BASE),
                        "publishDate",
                        SortDirection.ASC,
                        null,
                        null,
                        2),
                condition(null, null, null, null, null, "publishDate", SortDirection.ASC, "bad-date", BASE, 2),
                condition(null, null, null, null, null, "viewCount", SortDirection.ASC, "abc", BASE, 2),
                condition(null, null, null, null, null, "commentCount", SortDirection.ASC, "-1", BASE, 2),
                condition(null, null, null, null, null, "viewCount", SortDirection.ASC, "1", null, 2),
                condition(null, null, null, null, null, "viewCount", SortDirection.ASC, null, BASE, 2),
                condition(null, null, null, null, null, "publishDate", SortDirection.ASC, null, null, 0));
    }

    @ParameterizedTest
    @MethodSource("invalidConditions")
    void rejectsInvalidSearchParameters(ArticleSearchCondition condition) {
        assertThatThrownBy(() -> repository.search(condition))
                .isInstanceOfSatisfying(
                        MonewException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.INCORRECT_REQUEST));
    }

    @Test
    void emptyResultHasZeroCount() {
        ArticleSearchCondition condition =
                condition(null, null, null, null, null, "publishDate", SortDirection.ASC, null, null, 2);
        assertThat(repository.search(condition)).isEmpty();
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

    private void link(Article value, UUID interestId) {
        em.persist(ArticleInterest.create(em.getReference(Article.class, value.getId()), interestId));
    }

    private static LocalDateTime local(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static ArticleSearchCondition condition(
            String keyword,
            UUID interestId,
            List<ArticleSource> sources,
            LocalDateTime from,
            LocalDateTime to,
            String orderBy,
            SortDirection direction,
            String cursor,
            Instant after,
            int limit) {
        return new ArticleSearchCondition(
                keyword, interestId, sources, from, to, orderBy, direction, new CursorRequest(cursor, after, limit));
    }
}

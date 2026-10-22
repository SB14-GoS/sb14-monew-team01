package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleOrderBy;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.UserIdRequiredException;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.article.domain.repository.ArticleSearchCursor;
import com.sprint.monew.article.domain.repository.ArticleViewRepository;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    ArticleQueryRepository articleQueryRepository;

    @Mock
    ArticleViewRepository articleViewRepository;

    @InjectMocks
    ArticleService articleService;

    private static final Instant BASE = Instant.parse("2026-10-21T00:00:00Z");
    private static final UUID CURSOR_ARTICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("사용자 ID가 없으면 조회 없이 예외를 던진다")
    void rejectsMissingUserId() {
        assertThatThrownBy(() -> articleService.search(condition(ArticleOrderBy.PUBLISH_DATE), null))
                .isInstanceOf(UserIdRequiredException.class);

        verifyNoInteractions(articleQueryRepository, articleViewRepository);
    }

    @ParameterizedTest
    @CsvSource({"PUBLISH_DATE, 2026-10-21T00:00:02Z", "COMMENT_COUNT, 7", "VIEW_COUNT, 42"})
    @DisplayName("반환할 기사에만 읽음 여부를 반영하고 마지막 기사의 정렬 값으로 커서를 만든다")
    void returnsViewedStatusAndCursor(ArticleOrderBy orderBy, String expectedCursor) {
        Article first = article(3);
        Article last = article(2);
        Article extra = article(1);
        ReflectionTestUtils.setField(last, "commentCount", 7L);
        ReflectionTestUtils.setField(last, "viewCount", 42L);
        ArticleSearchCondition condition = condition(orderBy);
        given(articleQueryRepository.search(condition, null)).willReturn(List.of(first, last, extra));
        given(articleQueryRepository.countByCondition(condition)).willReturn(12L);
        given(articleViewRepository.findViewedArticleIds(userId, List.of(first.getId(), last.getId())))
                .willReturn(Set.of(first.getId()));

        CursorPageResponse<ArticleDto> result = articleService.search(condition, userId);

        assertThat(result.content()).extracting(ArticleDto::id).containsExactly(first.getId(), last.getId());
        assertThat(result.content()).extracting(ArticleDto::viewedByMe).containsExactly(true, false);
        ArticleDto lastDto = result.content().get(1);
        assertThat(lastDto.title()).isEqualTo(last.getTitle());
        assertThat(lastDto.publishDate()).isEqualTo(last.getPublishedDate());
        assertThat(lastDto.commentCount()).isEqualTo(7L);
        assertThat(lastDto.viewCount()).isEqualTo(42L);
        assertThat(result.nextCursor()).isEqualTo(expectedCursor + "|" + last.getId());
        assertThat(result.nextAfter()).isEqualTo(last.getCreatedAt());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(12L);
        assertThat(result.hasNext()).isTrue();
        verify(articleViewRepository).findViewedArticleIds(userId, List.of(first.getId(), last.getId()));
        verifyNoMoreInteractions(articleViewRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("조회 결과가 limit 이하면 마지막 페이지로 반환하고 읽음 기록이 없으면 false다")
    void returnsLastPage(int fetchedSize) {
        ArticleSearchCondition condition = condition(ArticleOrderBy.PUBLISH_DATE);
        List<Article> articles = List.of(article(2), article(1)).subList(0, fetchedSize);
        List<UUID> articleIds = articles.stream().map(Article::getId).toList();
        given(articleQueryRepository.search(condition, null)).willReturn(articles);
        given(articleQueryRepository.countByCondition(condition)).willReturn((long) fetchedSize);
        given(articleViewRepository.findViewedArticleIds(userId, articleIds)).willReturn(Set.of());

        CursorPageResponse<ArticleDto> result = articleService.search(condition, userId);

        assertThat(result.content()).extracting(ArticleDto::id).containsExactlyElementsOf(articleIds);
        assertThat(result.content())
                .allSatisfy(dto -> assertThat(dto.viewedByMe()).isFalse());
        assertThat(result.size()).isEqualTo(fetchedSize);
        assertThat(result.totalElements()).isEqualTo(fetchedSize);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextAfter()).isNull();
    }

    @Test
    @DisplayName("검색 결과가 없으면 읽음 기록을 조회하지 않고 빈 페이지를 반환한다")
    void returnsEmptyPageWithoutLookingUpViews() {
        ArticleSearchCondition condition = condition(ArticleOrderBy.PUBLISH_DATE);
        given(articleQueryRepository.search(condition, null)).willReturn(List.of());
        given(articleQueryRepository.countByCondition(condition)).willReturn(0L);

        CursorPageResponse<ArticleDto> result = articleService.search(condition, userId);

        assertThat(result.content()).isEmpty();
        assertThat(result.size()).isZero();
        assertThat(result.totalElements()).isZero();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextAfter()).isNull();
        verifyNoInteractions(articleViewRepository);
    }

    static Stream<Arguments> invalidConditions() {
        return Stream.of(
                Arguments.of(condition(null, SortDirection.ASC, new CursorRequest(null, null, 2)), "orderBy"),
                Arguments.of(
                        condition(ArticleOrderBy.PUBLISH_DATE, null, new CursorRequest(null, null, 2)), "direction"),
                Arguments.of(condition(ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC, null), "limit"),
                Arguments.of(
                        condition(ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC, new CursorRequest(null, null, null)),
                        "limit"),
                Arguments.of(
                        condition(ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC, new CursorRequest(null, null, 0)),
                        "limit"),
                Arguments.of(
                        condition(ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC, new CursorRequest(null, null, 101)),
                        "limit"),
                Arguments.of(
                        new ArticleSearchCondition(
                                null,
                                null,
                                null,
                                LocalDateTime.parse("2026-10-22T00:00:00"),
                                LocalDateTime.parse("2026-10-21T00:00:00"),
                                ArticleOrderBy.PUBLISH_DATE,
                                SortDirection.ASC,
                                new CursorRequest(null, null, 2)),
                        "publishDateFrom/publishDateTo"),
                Arguments.of(
                        condition(
                                ArticleOrderBy.PUBLISH_DATE,
                                SortDirection.ASC,
                                new CursorRequest(cursor("bad-date"), BASE, 2)),
                        "cursor"),
                Arguments.of(
                        condition(
                                ArticleOrderBy.VIEW_COUNT,
                                SortDirection.ASC,
                                new CursorRequest(cursor("abc"), BASE, 2)),
                        "cursor"),
                Arguments.of(
                        condition(
                                ArticleOrderBy.COMMENT_COUNT,
                                SortDirection.ASC,
                                new CursorRequest(cursor("-1"), BASE, 2)),
                        "cursor"),
                Arguments.of(
                        condition(
                                ArticleOrderBy.VIEW_COUNT,
                                SortDirection.ASC,
                                new CursorRequest(cursor("9223372036854775808"), BASE, 2)),
                        "cursor"),
                Arguments.of(
                        condition(
                                ArticleOrderBy.VIEW_COUNT, SortDirection.ASC, new CursorRequest(cursor("1"), null, 2)),
                        "cursor/after"),
                Arguments.of(
                        condition(ArticleOrderBy.VIEW_COUNT, SortDirection.ASC, new CursorRequest(null, BASE, 2)),
                        "cursor/after"),
                Arguments.of(
                        condition(ArticleOrderBy.VIEW_COUNT, SortDirection.ASC, new CursorRequest(" ", BASE, 2)),
                        "cursor/after"));
    }

    @ParameterizedTest
    @MethodSource("invalidConditions")
    @DisplayName("잘못된 검색 조건은 Repository 호출 전에 서비스에서 거부한다")
    void rejectsInvalidConditionsBeforeQuery(ArticleSearchCondition condition, String field) {
        assertThatThrownBy(() -> articleService.search(condition, userId))
                .isInstanceOfSatisfying(MonewException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.INCORRECT_REQUEST);
                    assertThat(error.getDetails()).containsEntry("field", field);
                });
        verifyNoInteractions(articleQueryRepository, articleViewRepository);
    }

    static Stream<Arguments> validCursors() {
        return Stream.of(
                Arguments.of(
                        ArticleOrderBy.PUBLISH_DATE,
                        cursor(BASE.toString()),
                        new ArticleSearchCursor(BASE, null, BASE, CURSOR_ARTICLE_ID)),
                Arguments.of(
                        ArticleOrderBy.COMMENT_COUNT,
                        cursor("7"),
                        new ArticleSearchCursor(null, 7L, BASE, CURSOR_ARTICLE_ID)),
                Arguments.of(
                        ArticleOrderBy.VIEW_COUNT,
                        cursor("0"),
                        new ArticleSearchCursor(null, 0L, BASE, CURSOR_ARTICLE_ID)));
    }

    @ParameterizedTest
    @MethodSource("validCursors")
    @DisplayName("유효한 커서는 서비스에서 날짜 또는 숫자로 변환해 Repository에 전달한다")
    void passesParsedCursorToRepository(ArticleOrderBy orderBy, String cursor, ArticleSearchCursor expected) {
        ArticleSearchCondition condition = condition(orderBy, SortDirection.DESC, new CursorRequest(cursor, BASE, 2));
        given(articleQueryRepository.search(condition, expected)).willReturn(List.of());
        given(articleQueryRepository.countByCondition(condition)).willReturn(0L);

        assertThat(articleService.search(condition, userId).content()).isEmpty();

        verify(articleQueryRepository).search(condition, expected);
        verifyNoInteractions(articleViewRepository);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1",
                "1|",
                "1|invalid",
                "1|1-1-1-1-1",
                "1|00000000-0000-0000-0000-000000000001|extra",
                "|00000000-0000-0000-0000-000000000001"
            })
    @DisplayName("기사 ID가 없거나 잘못된 복합 커서는 조회 전에 거부한다")
    void rejectsMalformedCompositeCursor(String cursor) {
        ArticleSearchCondition condition =
                condition(ArticleOrderBy.VIEW_COUNT, SortDirection.DESC, new CursorRequest(cursor, BASE, 2));
        assertThatThrownBy(() -> articleService.search(condition, userId))
                .isInstanceOfSatisfying(MonewException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.INCORRECT_REQUEST);
                    assertThat(error.getDetails()).containsEntry("field", "cursor");
                });
        verifyNoInteractions(articleQueryRepository, articleViewRepository);
    }

    private static String cursor(String value) {
        return value + "|" + CURSOR_ARTICLE_ID;
    }

    private static ArticleSearchCondition condition(
            ArticleOrderBy orderBy, SortDirection direction, CursorRequest page) {
        return new ArticleSearchCondition("경제", null, null, null, null, orderBy, direction, page);
    }

    private ArticleSearchCondition condition(ArticleOrderBy orderBy) {
        return new ArticleSearchCondition(
                "경제", null, null, null, null, orderBy, SortDirection.DESC, new CursorRequest(null, null, 2));
    }

    private Article article(int second) {
        Article article = Article.create(
                "경제 기사 " + second,
                BASE.plusSeconds(second),
                "요약",
                ArticleSource.NAVER,
                "https://news.example/" + second);
        ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(article, "createdAt", BASE.plusSeconds(100 + second));
        return article;
    }
}

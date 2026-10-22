package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.UserIdRequiredException;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.article.domain.repository.ArticleViewRepository;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("사용자 ID가 없으면 조회 없이 예외를 던진다")
    void rejectsMissingUserId() {
        assertThatThrownBy(() -> articleService.search(condition("publishDate"), null))
                .isInstanceOf(UserIdRequiredException.class);

        verifyNoInteractions(articleQueryRepository, articleViewRepository);
    }

    @ParameterizedTest
    @CsvSource({"publishDate, 2026-10-21T00:00:02Z", "commentCount, 7", "viewCount, 42"})
    @DisplayName("반환할 기사에만 읽음 여부를 반영하고 마지막 기사의 정렬 값으로 커서를 만든다")
    void returnsViewedStatusAndCursor(String orderBy, String expectedCursor) {
        Article first = article(3);
        Article last = article(2);
        Article extra = article(1);
        ReflectionTestUtils.setField(last, "commentCount", 7L);
        ReflectionTestUtils.setField(last, "viewCount", 42L);
        ArticleSearchCondition condition = condition(orderBy);
        given(articleQueryRepository.search(condition)).willReturn(List.of(first, last, extra));
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
        assertThat(result.nextCursor()).isEqualTo(expectedCursor);
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
        ArticleSearchCondition condition = condition("publishDate");
        List<Article> articles = List.of(article(2), article(1)).subList(0, fetchedSize);
        List<UUID> articleIds = articles.stream().map(Article::getId).toList();
        given(articleQueryRepository.search(condition)).willReturn(articles);
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
        ArticleSearchCondition condition = condition("publishDate");
        given(articleQueryRepository.search(condition)).willReturn(List.of());
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

    private ArticleSearchCondition condition(String orderBy) {
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

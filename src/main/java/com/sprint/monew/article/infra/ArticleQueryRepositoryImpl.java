package com.sprint.monew.article.infra;

import static com.sprint.monew.article.domain.QArticle.article;
import static com.sprint.monew.article.domain.QArticleInterest.articleInterest;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.QArticle;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Article> search(ArticleSearchCondition condition) {
        validatePage(condition);
        validateDateRange(condition);
        return jpaQueryFactory
                .selectFrom(QArticle.article)
                .where(
                        article.deletedAt.isNull(),
                        keywordContains(condition.keyword()),
                        interestEquals(condition.interestId()),
                        sourceIn(condition.sourceIn()),
                        publishDateFrom(condition.publishDateFrom()),
                        publishDateTo(condition.publishDateTo()),
                        cursorAfter(condition))
                .orderBy(orderBy(condition))
                .limit(condition.page().limit() + 1L)
                .fetch();
    }

    @Override
    public long countByCondition(ArticleSearchCondition condition) {
        validateDateRange(condition);
        Long result = jpaQueryFactory
                .select(QArticle.article.count())
                .from(article)
                .where(
                        article.deletedAt.isNull(), /* 논리삭제 안된애들만 */
                        keywordContains(condition.keyword()),
                        interestEquals(condition.interestId()),
                        sourceIn(condition.sourceIn()),
                        publishDateFrom(condition.publishDateFrom()),
                        publishDateTo(condition.publishDateTo()))
                .fetchOne();
        return result == null ? 0L : result;
    }
    /* 검색조건 검사 */
    private void validatePage(ArticleSearchCondition condition) {
        if (condition.orderBy() == null) {
            throw invalidRequest("orderBy");
        }
        if (condition.direction() == null) {
            throw invalidRequest("direction");
        }
        if (condition.page() == null
                || condition.page().limit() == null
                || condition.page().limit() < 1) {
            throw invalidRequest("limit");
        }
    }
    /* 검색조건 검사 (범위) */
    private void validateDateRange(ArticleSearchCondition condition) {
        if (condition.publishDateFrom() != null
                && condition.publishDateTo() != null
                && condition.publishDateFrom().isAfter(condition.publishDateTo())) {
            throw invalidRequest("publishDateFrom/publishDateTo");
        }
    }

    /* 키워드 포함 조건 */
    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return article.title.contains(keyword).or(article.summary.contains(keyword));
    }

    /* 관심사 포함 조건*/
    private BooleanExpression interestEquals(UUID interestId) {
        if (interestId == null) {
            return null;
        }
        return JPAExpressions.selectOne()
                .from(articleInterest)
                .where(articleInterest.article.eq(article), articleInterest.interestId.eq(interestId))
                .exists();
    }

    /* 출처 조건 */
    private BooleanExpression sourceIn(List<ArticleSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        return article.source.in(sources);
    }

    /* 기사 등록일 */
    private BooleanExpression publishDateFrom(LocalDateTime from) {
        return from == null ? null : article.publishedDate.goe(from.toInstant(ZoneOffset.UTC));
    }

    private BooleanExpression publishDateTo(LocalDateTime to) {
        return to == null ? null : article.publishedDate.loe(to.toInstant(ZoneOffset.UTC));
    }

    /* 어떤걸 기준으로 정렬할지 */
    private OrderSpecifier<?>[] orderBy(ArticleSearchCondition condition) {
        boolean ascending = condition.direction() == SortDirection.ASC;
        OrderSpecifier<?> primary = switch (condition.orderBy()) {
            /* 어떤 기준을 담을지 */
            case "publishDate" -> ascending ? article.publishedDate.asc() : article.publishedDate.desc();
            case "commentCount" -> ascending ? article.commentCount.asc() : article.commentCount.desc();
            case "viewCount" -> ascending ? article.viewCount.asc() : article.viewCount.desc();
            default -> throw invalidRequest("orderBy");
        };
        return new OrderSpecifier<?>[] {primary, ascending ? article.createdAt.asc() : article.createdAt.desc()};
    }

    // shortcut: 정렬값과 createdAt이 모두 같으면 구분할 수 없다. 이 경우 커서에 UUID를 추가해야 한다.
    private BooleanExpression cursorAfter(ArticleSearchCondition condition) {
        CursorRequest page = condition.page();
        boolean hasCursor = page.cursor() != null && !page.cursor().isBlank();
        if (!hasCursor && page.after() == null) {
            return null;
        }
        if (!hasCursor || page.after() == null) {
            throw invalidRequest("cursor/after");
        }

        boolean ascending = condition.direction() == SortDirection.ASC;
        BooleanExpression createdAfter =
                ascending ? article.createdAt.gt(page.after()) : article.createdAt.lt(page.after());
        try {
            return switch (condition.orderBy()) {
                case "publishDate" -> {
                    Instant value = Instant.parse(page.cursor());
                    BooleanExpression beyond =
                            ascending ? article.publishedDate.gt(value) : article.publishedDate.lt(value);
                    yield beyond.or(article.publishedDate.eq(value).and(createdAfter));
                }
                case "commentCount" -> countAfter(article.commentCount, page.cursor(), ascending, createdAfter);
                case "viewCount" -> countAfter(article.viewCount, page.cursor(), ascending, createdAfter);
                default -> throw invalidRequest("orderBy");
            };
        } catch (DateTimeParseException | NumberFormatException e) {
            throw invalidRequest("cursor");
        }
    }

    private BooleanExpression countAfter(
            NumberPath<Long> field, String cursor, boolean ascending, BooleanExpression createdAfter) {
        long value = Long.parseLong(cursor);
        if (value < 0) {
            throw invalidRequest("cursor");
        }
        BooleanExpression beyond = ascending ? field.gt(value) : field.lt(value);
        return beyond.or(field.eq(value).and(createdAfter));
    }

    private MonewException invalidRequest(String field) {
        return new MonewException(CommonErrorCode.INCORRECT_REQUEST, Map.of("field", field));
    }
}

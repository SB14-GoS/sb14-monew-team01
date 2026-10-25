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
import com.sprint.monew.article.domain.repository.ArticleSearchCursor;
import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Article> search(ArticleSearchCondition condition, ArticleSearchCursor cursor) {
        return jpaQueryFactory
                .selectFrom(QArticle.article)
                .where(
                        article.deletedAt.isNull(),
                        keywordContains(condition.keyword()),
                        interestEquals(condition.interestId()),
                        sourceIn(condition.sourceIn()),
                        publishDateFrom(condition.publishDateFrom()),
                        publishDateTo(condition.publishDateTo()),
                        cursorAfter(condition, cursor))
                .orderBy(orderBy(condition))
                .limit(condition.page().limit() + 1L)
                .fetch();
    }

    @Override
    public long countByCondition(ArticleSearchCondition condition) {
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
        return from == null
                ? null
                : article.publishedDate.goe(from.atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }

    private BooleanExpression publishDateTo(LocalDateTime to) {
        return to == null
                ? null
                : article.publishedDate.loe(to.atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }

    /* 어떤걸 기준으로 정렬할지 */
    private OrderSpecifier<?>[] orderBy(ArticleSearchCondition condition) {
        boolean ascending = condition.direction() == SortDirection.ASC;
        OrderSpecifier<?> primary = switch (condition.orderBy()) {
            /* 어떤 기준을 담을지 */
            case PUBLISH_DATE -> ascending ? article.publishedDate.asc() : article.publishedDate.desc();
            case COMMENT_COUNT -> ascending ? article.commentCount.asc() : article.commentCount.desc();
            case VIEW_COUNT -> ascending ? article.viewCount.asc() : article.viewCount.desc();
        };
        return new OrderSpecifier<?>[] {
            primary,
            ascending ? article.createdAt.asc() : article.createdAt.desc(),
            ascending ? article.id.asc() : article.id.desc()
        };
    }

    private BooleanExpression cursorAfter(ArticleSearchCondition condition, ArticleSearchCursor cursor) {
        if (cursor == null) {
            return null;
        }
        boolean ascending = condition.direction() == SortDirection.ASC;
        BooleanExpression idAfter = ascending ? article.id.gt(cursor.articleId()) : article.id.lt(cursor.articleId());
        BooleanExpression createdAfter = (ascending
                        ? article.createdAt.gt(cursor.after())
                        : article.createdAt.lt(cursor.after()))
                .or(article.createdAt.eq(cursor.after()).and(idAfter));
        return switch (condition.orderBy()) {
            case PUBLISH_DATE -> {
                Instant value = cursor.publishDate();
                BooleanExpression beyond =
                        ascending ? article.publishedDate.gt(value) : article.publishedDate.lt(value);
                yield beyond.or(article.publishedDate.eq(value).and(createdAfter));
            }
            case COMMENT_COUNT -> countAfter(article.commentCount, cursor.count(), ascending, createdAfter);
            case VIEW_COUNT -> countAfter(article.viewCount, cursor.count(), ascending, createdAfter);
        };
    }

    private BooleanExpression countAfter(
            NumberPath<Long> field, long value, boolean ascending, BooleanExpression createdAfter) {
        BooleanExpression beyond = ascending ? field.gt(value) : field.lt(value);
        return beyond.or(field.eq(value).and(createdAfter));
    }
}

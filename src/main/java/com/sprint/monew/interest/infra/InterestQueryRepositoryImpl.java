package com.sprint.monew.interest.infra;

import static com.sprint.monew.interest.domain.QInterest.interest;
import static com.sprint.monew.interest.domain.QInterestKeyword.interestKeyword;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.interest.domain.Interest;
import com.sprint.monew.interest.domain.InterestQueryRepository;
import com.sprint.monew.interest.domain.InterestSearchCondition;
import com.sprint.monew.interest.domain.InterestSearchCursor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class InterestQueryRepositoryImpl implements InterestQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Interest> search(InterestSearchCondition condition, InterestSearchCursor cursor) {
        List<Interest> fetched = queryFactory
                .selectFrom(interest)
                .where(containsKeyword(condition.keyword()), cursorAfter(cursor))
                .orderBy(orderBy(cursor))
                .limit(cursor.limit() + 1L)
                .fetch();

        if (!fetched.isEmpty()) {
            List<UUID> ids = fetched.stream().map(Interest::getId).toList();
            queryFactory
                    .selectFrom(interest)
                    .leftJoin(interest.keywords, interestKeyword)
                    .fetchJoin()
                    .where(interest.id.in(ids))
                    .fetch();
        }
        return fetched;
    }

    @Override
    public long countByCondition(InterestSearchCondition condition) {
        Long count = queryFactory
                .select(interest.count())
                .from(interest)
                .where(containsKeyword(condition.keyword()))
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return interest.name
                .containsIgnoreCase(keyword)
                .or(JPAExpressions.selectOne()
                        .from(interestKeyword)
                        .where(
                                interestKeyword.interest.eq(interest),
                                interestKeyword.keyword.containsIgnoreCase(keyword))
                        .exists());
    }

    private BooleanExpression cursorAfter(InterestSearchCursor cursor) {
        if (cursor == null) {
            return null;
        }

        boolean hasCursor = StringUtils.hasText(cursor.cursor());
        boolean hasAfter = cursor.after() != null;

        if (!hasCursor && !hasAfter) {
            return null;
        }

        if (!hasCursor || !hasAfter) {
            throw new MonewException(
                    CommonErrorCode.INCORRECT_REQUEST, Map.of("cursor/after", "must be provided together"));
        }

        boolean ascending = cursor.direction() == SortDirection.ASC;

        if ("subscriberCount".equals(cursor.orderBy())) {
            ParsedSubscriberCursor parsed = parseSubscriberCountCursor(cursor.cursor());

            BooleanExpression idAfter = ascending ? interest.id.gt(parsed.id()) : interest.id.lt(parsed.id());

            BooleanExpression createdAfter = (ascending
                            ? interest.createdAt.gt(cursor.after())
                            : interest.createdAt.lt(cursor.after()))
                    .or(interest.createdAt.eq(cursor.after()).and(idAfter));

            return countAfter(interest.subscriberCount, parsed.count(), ascending, createdAfter);
        }

        BooleanExpression createdAfterForName =
                ascending ? interest.createdAt.gt(cursor.after()) : interest.createdAt.lt(cursor.after());

        BooleanExpression nameBeyond =
                ascending ? interest.name.gt(cursor.cursor()) : interest.name.lt(cursor.cursor());

        return nameBeyond.or(interest.name.eq(cursor.cursor()).and(createdAfterForName));
    }

    private BooleanExpression countAfter(
            NumberPath<Long> field, long value, boolean ascending, BooleanExpression createdAfter) {
        BooleanExpression beyond = ascending ? field.gt(value) : field.lt(value);
        return beyond.or(field.eq(value).and(createdAfter));
    }

    private record ParsedSubscriberCursor(long count, UUID id) {}

    private ParsedSubscriberCursor parseSubscriberCountCursor(String cursorString) {
        try {
            String[] parts = cursorString.split("_");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            return new ParsedSubscriberCursor(Long.parseLong(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            throw new MonewException(CommonErrorCode.INCORRECT_REQUEST, Map.of("cursor", cursorString));
        }
    }

    private OrderSpecifier<?>[] orderBy(InterestSearchCursor cursor) {
        if (cursor == null) {
            return new OrderSpecifier<?>[] {interest.createdAt.desc(), interest.id.desc()};
        }

        boolean isAsc = cursor.direction() == SortDirection.ASC;
        OrderSpecifier<?> primary = "subscriberCount".equals(cursor.orderBy())
                ? (isAsc ? interest.subscriberCount.asc() : interest.subscriberCount.desc())
                : (isAsc ? interest.name.asc() : interest.name.desc());

        return new OrderSpecifier<?>[] {
            primary,
            isAsc ? interest.createdAt.asc() : interest.createdAt.desc(),
            isAsc ? interest.id.asc() : interest.id.desc()
        };
    }
}

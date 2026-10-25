package com.sprint.monew.comment.infra;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentOrderBy;
import com.sprint.monew.comment.domain.CommentQueryRepository;
import com.sprint.monew.comment.domain.CommentSearchResult;
import com.sprint.monew.comment.domain.QComment;
import com.sprint.monew.comment.domain.QCommentLike;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 특정 기사에 달린 댓글을 정렬하여, 일정 개수씩 조회하는 기능, 한번에 limit 개씩 조회
    @Override
    public List<CommentSearchResult> findByArticleId(
            UUID articleId, UUID requesterId, CommentOrderBy orderBy, SortDirection direction, CursorRequest request) {

        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;

        BooleanExpression likedByMe = commentLike.id.isNotNull();

        List<Tuple> results = queryFactory
                .select(comment, likedByMe)
                .from(comment)
                .leftJoin(comment.commentLike, commentLike)
                .on(commentLike.userId.eq(requesterId))
                .where(
                        comment.articleId.eq(articleId),
                        comment.deletedAt.isNull(),
                        cursorCondition(comment, articleId, request, orderBy, direction))
                .orderBy(orderSpecifiers(comment, orderBy, direction))
                .limit(request.limit() + 1L)
                .fetch();

        return results.stream()
                .map(tuple -> new CommentSearchResult(tuple.get(comment), Boolean.TRUE.equals(tuple.get(likedByMe))))
                .toList();
    }

    @Override
    public long countByArticleId(UUID articleId) {
        QComment comment = QComment.comment;

        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.articleId.eq(articleId), comment.deletedAt.isNull())
                .fetchOne();
        return count == null ? 0L : count;
    }

    // 첫 페이지면 조건 없음. 다음 페이지면 "마지막으로 본 댓글 다음"부터
    private BooleanExpression cursorCondition(
            QComment comment, UUID articleId, CursorRequest request, CommentOrderBy orderBy, SortDirection direction) {

        if (request.cursor() == null) {
            return null;
        }

        return switch (orderBy) {
            case CREATED_AT -> createdAtCursorCondition(comment, request, direction);
            case LIKE_COUNT -> likeCountCursorCondition(comment, articleId, request, direction);
        };
    }

    private OrderSpecifier<?>[] orderSpecifiers( // 생성순, 좋아요 순서로 댓글 정렬
            QComment comment, CommentOrderBy orderBy, SortDirection direction) {

        boolean asc = direction == SortDirection.ASC;

        return switch (orderBy) {
            case CREATED_AT ->
                asc
                        ? new OrderSpecifier<?>[] {comment.createdAt.asc(), comment.id.asc()}
                        : new OrderSpecifier<?>[] {comment.createdAt.desc(), comment.id.desc()};

            case LIKE_COUNT ->
                asc
                        ? new OrderSpecifier<?>[] {comment.likeCount.asc(), comment.id.asc()}
                        : new OrderSpecifier<?>[] {comment.likeCount.desc(), comment.id.desc()};
        };
    }

    private BooleanExpression likeCountCursorCondition(
            QComment comment, UUID articleId, CursorRequest request, SortDirection direction) {

        if (request.cursor() == null) {
            return null;
        }

        UUID cursorId = UUID.fromString(request.cursor());

        Comment cursorComment = queryFactory
                .selectFrom(comment)
                .where(comment.id.eq(cursorId), comment.articleId.eq(articleId), comment.deletedAt.isNull())
                .fetchOne();

        // 실제 구현에서는 없을 때 예외 처리
        if (cursorComment == null) {
            throw new IllegalArgumentException("유효하지 않은 cursor입니다.");
        }

        long cursorLikeCount = cursorComment.getLikeCount();
        boolean asc = direction == SortDirection.ASC;

        return asc
                ? comment.likeCount
                        .gt(cursorLikeCount)
                        .or(comment.likeCount.eq(cursorLikeCount).and(comment.id.gt(cursorId)))
                : comment.likeCount
                        .lt(cursorLikeCount)
                        .or(comment.likeCount.eq(cursorLikeCount).and(comment.id.lt(cursorId)));
    }

    private BooleanExpression createdAtCursorCondition(
            QComment comment, CursorRequest request, SortDirection direction) {

        if (request.after() == null) {
            return null;
        }

        UUID cursorId = UUID.fromString(request.cursor());
        Instant after = request.after();
        boolean asc = direction == SortDirection.ASC;

        return asc
                ? comment.createdAt.gt(after).or(comment.createdAt.eq(after).and(comment.id.gt(cursorId)))
                : comment.createdAt.lt(after).or(comment.createdAt.eq(after).and(comment.id.lt(cursorId)));
    }
}

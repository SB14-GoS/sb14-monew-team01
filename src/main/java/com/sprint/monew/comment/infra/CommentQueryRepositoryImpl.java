package com.sprint.monew.comment.infra;

import static com.sprint.monew.comment.domain.QCommentLike.commentLike;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
    // 기사별 댓글과 요청자 기준 좋아요 여부를 커서 조건에 맞춰 조회한다.
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
                        cursorCondition(comment, request, orderBy, direction))
                .orderBy(orderSpecifiers(comment, orderBy, direction))
                .limit(request.limit() + 1L)
                .fetch();

        return results.stream()
                .map(tuple -> new CommentSearchResult(tuple.get(comment), Boolean.TRUE.equals(tuple.get(likedByMe))))
                .toList();
    }

    @Override
    // 소프트 삭제되지 않은 기사 댓글의 전체 개수를 조회한다.
    public long countByArticleId(UUID articleId) {
        QComment comment = QComment.comment;

        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.articleId.eq(articleId), comment.deletedAt.isNull())
                .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public boolean existsByCommentIdAndUserId(UUID commentId, UUID userId) {
        return queryFactory
                        .selectOne()
                        .from(commentLike)
                        .where(commentLike.comment.id.eq(commentId), commentLike.userId.eq(userId))
                        .fetchFirst()
                != null;
    }

    // ========================================================================================
    // 내부 헬퍼 메서드

    // 첫 페이지는 비워 두고 다음 페이지는 정렬 기준에 맞는 cursor 조건을 반환한다.
    private BooleanExpression cursorCondition(
            QComment comment, CursorRequest request, CommentOrderBy orderBy, SortDirection direction) {
        if (request.cursor() == null) {
            return null;
        }

        return switch (orderBy) {
            case CREATED_AT -> createdAtCursorCondition(comment, request, direction);
            case LIKE_COUNT -> likeCountCursorCondition(comment, request, direction);
        };
    }

    // 정렬 기준과 방향에 따른 기본 정렬 및 ID 보조 정렬을 반환한다.
    private OrderSpecifier<?>[] orderSpecifiers(QComment comment, CommentOrderBy orderBy, SortDirection direction) {

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

    // 좋아요 수와 댓글 ID를 기준으로 다음 페이지 조건을 생성한다.
    private BooleanExpression likeCountCursorCondition(
            QComment comment, CursorRequest request, SortDirection direction) {
        boolean asc = direction == SortDirection.ASC;

        LikeCountCursor cursor = parseLikeCountCursor(request.cursor());

        return asc
                ? comment.likeCount
                        .gt(cursor.likeCount())
                        .or(comment.likeCount.eq(cursor.likeCount()).and(comment.id.gt(cursor.commentId())))
                : comment.likeCount
                        .lt(cursor.likeCount())
                        .or(comment.likeCount.eq(cursor.likeCount()).and(comment.id.lt(cursor.commentId())));
    }

    // 생성 시각과 댓글 ID를 기준으로 다음 페이지 조건을 생성한다.
    private BooleanExpression createdAtCursorCondition(
            QComment comment, CursorRequest request, SortDirection direction) {
        UUID cursorId = parseCursorId(request.cursor());
        Instant after = request.after();
        boolean asc = direction == SortDirection.ASC;

        return asc
                ? comment.createdAt.gt(after).or(comment.createdAt.eq(after).and(comment.id.gt(cursorId)))
                : comment.createdAt.lt(after).or(comment.createdAt.eq(after).and(comment.id.lt(cursorId)));
    }

    // 생성 시각 정렬 cursor의 댓글 ID를 UUID로 변환한다.
    private UUID parseCursorId(String cursor) {
        return UUID.fromString(cursor);
    }

    // 좋아요 수 정렬 cursor의 "likeCount_commentId" 값을 분리한다.
    private LikeCountCursor parseLikeCountCursor(String cursor) {
        String[] values = cursor.split("_", 2);

        if (values.length != 2) {
            throw new IllegalArgumentException();
        }

        return new LikeCountCursor(Long.parseLong(values[0]), UUID.fromString(values[1]));
    }

    // 좋아요 수 정렬의 커서 기준값을 보관한다.
    private record LikeCountCursor(long likeCount, UUID commentId) {}
}

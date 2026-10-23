package com.sprint.monew.comment.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentQueryRepository;
import com.sprint.monew.comment.domain.QComment;
import com.sprint.monew.global.common.CursorRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 특정 기사에 달린 댓글을 정렬하여, 일정 개수씩 조회하는 기능, 한번에 limit 개씩 조회
    public List<Comment> findByArticleId(UUID articleId, CursorRequest request) {
        QComment comment = QComment.comment;

        return queryFactory
                .selectFrom(comment) // 댓글 테이블(comment)을 조회 대상으로 정합니다.
                .where(
                        // 댓글 DB에서 article_id 값이 요청받은 articleId와 같은 댓글만 고릅니다.
                        comment.articleId.eq(articleId),
                        // deletedAt은 댓글이 논리 삭제되었을 때 삭제 시각이 기록되는 컬럼입니다.
                        comment.deletedAt.isNull())
                .orderBy(comment.createdAt.asc()) // 오름 차순 정렬
                .limit(request.limit() + 1L)
                .fetch(); // 실제 쿼리 실행
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
}

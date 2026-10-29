package com.sprint.monew.comment.domain.repository;

import com.sprint.monew.comment.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentQueryRepository {

    List<Comment> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    /** 기사에 속한 모든 댓글을 물리 삭제한다. */
    void deleteAllByArticleId(UUID articleId);

    /** 기사에 속한 모든 댓글을 조회한다. */
    List<Comment> findAllByArticleId(UUID articleId);

    @Modifying
    @Query("""
        UPDATE Comment c
        SET c.deletedAt = CURRENT_TIMESTAMP
        WHERE c.id = :commentId
          AND c.deletedAt IS NULL
        """)
    int softDeleteIfActive(@Param("commentId") UUID commentId);

    @Modifying
    @Query("""
    DELETE FROM Comment c
    WHERE c.id = :commentId
      AND c.deletedAt IS NULL
    """)
    int hardDeleteIfActive(@Param("commentId") UUID commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    int increaseLikeCount(@Param("commentId") UUID commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Comment c
    SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END
    WHERE c.id = :commentId
    """)
    int decreaseLikeCount(@Param("commentId") UUID commentId);
}

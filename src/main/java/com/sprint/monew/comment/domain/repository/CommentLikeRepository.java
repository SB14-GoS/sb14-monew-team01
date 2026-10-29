package com.sprint.monew.comment.domain.repository;

import com.sprint.monew.comment.domain.CommentLike;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    List<CommentLike> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.userId = :userId")
    int deleteByCommentIdAndUserId(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

    /** 사용자가 해당 댓글에 좋아요를 눌렀는지 확인한다. */
    boolean existsByComment_IdAndUserId(UUID commentId, UUID userId);

    /** 기사에 속한 모든 댓글 좋아요를 물리 삭제한다. */
    void deleteAllByArticleId(UUID articleId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    int deleteAllByCommentId(@Param("commentId") UUID commentId);

    /** 댓글 ID 목록에 해당하는 모든 좋아요를 물리 삭제한다. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id IN :commentIds")
    int deleteAllByCommentIdIn(@Param("commentIds") List<UUID> commentIds);
}

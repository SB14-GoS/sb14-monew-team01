package com.sprint.monew.comment.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByComment_IdAndUserId(UUID commentId, UUID requesterUserId);

    @Modifying
    @Query("""
    DELETE FROM CommentLike cl
    WHERE cl.comment.id = :commentId
      AND cl.userId = :userId
    """)
    int deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}

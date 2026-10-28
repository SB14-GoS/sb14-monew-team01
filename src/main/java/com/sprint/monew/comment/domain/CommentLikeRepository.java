package com.sprint.monew.comment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByComment_IdAndUserId(UUID commentId, UUID requesterUserId);

    List<CommentLike> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    @Modifying
    @Query("""
    DELETE FROM CommentLike cl
    WHERE cl.comment.id = :commentId
      AND cl.userId = :userId
    """)
    int deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}

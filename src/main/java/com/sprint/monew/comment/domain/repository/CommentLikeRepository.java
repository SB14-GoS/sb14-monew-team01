package com.sprint.monew.comment.domain.repository;

import com.sprint.monew.comment.domain.CommentLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    /** 사용자가 해당 댓글에 좋아요를 눌렀는지 확인한다. */
    boolean existsByComment_IdAndUserId(UUID commentId, UUID userId);

    /** 기사에 속한 모든 댓글 좋아요를 물리 삭제한다. */
    void deleteAllByArticleId(UUID articleId);
}

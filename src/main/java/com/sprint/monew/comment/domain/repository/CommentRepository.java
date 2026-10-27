package com.sprint.monew.comment.domain.repository;

import com.sprint.monew.comment.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentQueryRepository {

    /** 기사에 속한 모든 댓글을 물리 삭제한다. */
    void deleteAllByArticleId(UUID articleId);

    /** 기사에 속한 모든 댓글을 조회한다. */
    List<Comment> findAllByArticleId(UUID articleId);
}

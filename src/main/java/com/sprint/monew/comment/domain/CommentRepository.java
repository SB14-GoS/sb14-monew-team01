package com.sprint.monew.comment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentQueryRepository {

    void deleteAllByArticleId(UUID articleId);

    List<Comment> findAllByArticleId(UUID articleId);
}

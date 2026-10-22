package com.sprint.monew.comment.domain;

import com.sprint.monew.global.common.CursorRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentQueryRepository {

    List<Comment> findByArticleId(UUID articleId, CursorRequest request);

    long countByArticleId(UUID articleId);
}

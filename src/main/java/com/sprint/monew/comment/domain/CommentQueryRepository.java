package com.sprint.monew.comment.domain;

import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentQueryRepository {

    List<CommentSearchResult> findByArticleId(
            UUID articleId, UUID requesterId, CommentOrderBy orderBy, SortDirection direction, CursorRequest request);

    long countByArticleId(UUID articleId);
}

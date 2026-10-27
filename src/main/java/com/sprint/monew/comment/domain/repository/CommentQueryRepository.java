package com.sprint.monew.comment.domain.repository;

import com.sprint.monew.comment.domain.CommentOrderBy;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.util.List;
import java.util.UUID;

public interface CommentQueryRepository {

    List<CommentSearchResult> findByArticleId(
            UUID articleId, UUID requesterId, CommentOrderBy orderBy, SortDirection direction, CursorRequest request);

    long countByArticleId(UUID articleId);
}

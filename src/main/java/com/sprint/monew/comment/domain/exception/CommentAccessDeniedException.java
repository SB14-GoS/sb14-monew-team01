package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;
import java.util.UUID;

public class CommentAccessDeniedException extends CommentException {

    public CommentAccessDeniedException(UUID commentId, UUID userId) {
        super(CommentErrorCode.COMMENT_ACCESS_DENIED, Map.of("commentId", commentId, "userId", userId));
    }
}

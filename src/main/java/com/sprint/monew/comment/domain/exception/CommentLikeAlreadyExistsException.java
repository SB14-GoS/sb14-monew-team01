package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;
import java.util.UUID;

public class CommentLikeAlreadyExistsException extends CommentException {

    public CommentLikeAlreadyExistsException(UUID commentId, UUID userId) {
        super(CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS, Map.of("commentId", commentId, "userId", userId));
    }

    public CommentLikeAlreadyExistsException(UUID commentId, UUID userId, Throwable cause) {
        this(commentId, userId);
        initCause(cause);
    }
}

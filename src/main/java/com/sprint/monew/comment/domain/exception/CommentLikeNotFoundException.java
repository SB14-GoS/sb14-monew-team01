package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;
import java.util.UUID;

public class CommentLikeNotFoundException extends CommentException {

    public CommentLikeNotFoundException(UUID commentId, UUID userId) {
        super(CommentErrorCode.COMMENT_LIKE_NOT_FOUND, Map.of("commentId", commentId, "userId", userId));
    }
}

package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;
import java.util.UUID;

public class CommentNotFoundException extends CommentException {

    public CommentNotFoundException(UUID commentId) {
        super(CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId));
    }
}

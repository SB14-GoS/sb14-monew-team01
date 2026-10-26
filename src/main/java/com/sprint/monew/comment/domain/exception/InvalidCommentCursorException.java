package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;

public class InvalidCommentCursorException extends CommentException {

    public InvalidCommentCursorException(String cursor) {
        super(CommentErrorCode.INVALID_COMMENT_CURSOR, Map.of("cursor", cursor == null ? "(null)" : cursor));
    }
}

package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;

public class InvalidCommentOrderByException extends CommentException {

    public InvalidCommentOrderByException(String orderBy) {
        super(CommentErrorCode.INVALID_COMMENT_ORDER_BY, Map.of("orderBy", orderBy == null ? "(null)" : orderBy));
    }
}

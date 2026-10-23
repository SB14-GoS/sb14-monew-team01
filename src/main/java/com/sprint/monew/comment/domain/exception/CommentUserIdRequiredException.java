package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;

public class CommentUserIdRequiredException extends CommentException {

    public CommentUserIdRequiredException() {
        super(CommentErrorCode.USER_ID_REQUIRED, Map.of());
    }
}

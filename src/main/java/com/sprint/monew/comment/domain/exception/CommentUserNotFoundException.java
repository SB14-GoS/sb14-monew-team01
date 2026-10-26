package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;
import java.util.UUID;

public class CommentUserNotFoundException extends CommentException {

    public CommentUserNotFoundException(UUID userId) {
        super(CommentErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
}

package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;

public class InvalidCommentContentException extends CommentException {

    public InvalidCommentContentException(String content) {
        super(CommentErrorCode.INVALID_COMMENT_CONTENT, Map.of("content", content == null ? "(null)" : content));
    }
}

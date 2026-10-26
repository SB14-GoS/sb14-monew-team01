package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;

public class ArticleIdRequiredException extends CommentException {

    public ArticleIdRequiredException() {
        super(CommentErrorCode.ARTICLE_ID_REQUIRED, Map.of());
    }
}

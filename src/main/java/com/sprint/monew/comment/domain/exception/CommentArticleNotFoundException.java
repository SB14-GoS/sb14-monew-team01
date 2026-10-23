package com.sprint.monew.comment.domain.exception;

import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import java.util.Map;
import java.util.UUID;

public class CommentArticleNotFoundException extends CommentException {

    public CommentArticleNotFoundException(UUID articleId) {
        super(CommentErrorCode.ARTICLE_NOT_FOUND, Map.of("articleId", articleId));
    }
}

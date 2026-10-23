package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class InvalidArticleTitleException extends ArticleException {

    public InvalidArticleTitleException(String title) {
        super(ArticleErrorCode.INVALID_ARTICLE_TITLE, Map.of("title", title == null ? "(null)" : title));
    }
}

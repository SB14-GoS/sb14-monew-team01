package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;
import java.util.UUID;

public class ArticleNotFoundException extends ArticleException {

    public ArticleNotFoundException(UUID articleId) {
        super(ArticleErrorCode.ARTICLE_NOT_FOUND, Map.of("articleId", articleId));
    }
}

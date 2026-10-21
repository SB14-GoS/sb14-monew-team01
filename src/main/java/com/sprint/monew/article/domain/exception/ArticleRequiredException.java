package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class ArticleRequiredException extends ArticleException {

    public ArticleRequiredException() {
        super(ArticleErrorCode.ARTICLE_REQUIRED, Map.of());
    }
}

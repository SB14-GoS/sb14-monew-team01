package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class ArticleSourceRequiredException extends ArticleException {

    public ArticleSourceRequiredException() {
        super(ArticleErrorCode.ARTICLE_SOURCE_REQUIRED, Map.of());
    }
}

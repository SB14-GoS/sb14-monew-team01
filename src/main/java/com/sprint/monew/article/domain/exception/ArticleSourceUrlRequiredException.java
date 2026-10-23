package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class ArticleSourceUrlRequiredException extends ArticleException {

    public ArticleSourceUrlRequiredException() {
        super(ArticleErrorCode.ARTICLE_SOURCE_URL_REQUIRED, Map.of());
    }
}

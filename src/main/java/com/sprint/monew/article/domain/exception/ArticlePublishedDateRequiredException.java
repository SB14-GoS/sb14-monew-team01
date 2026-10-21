package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class ArticlePublishedDateRequiredException extends ArticleException {

    public ArticlePublishedDateRequiredException() {
        super(ArticleErrorCode.ARTICLE_PUBLISHED_DATE_REQUIRED, Map.of());
    }
}

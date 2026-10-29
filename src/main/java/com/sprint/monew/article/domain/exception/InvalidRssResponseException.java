package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class InvalidRssResponseException extends ArticleException {

    public InvalidRssResponseException() {
        super(ArticleErrorCode.INVALID_RSS_RESPONSE, Map.of());
    }
}

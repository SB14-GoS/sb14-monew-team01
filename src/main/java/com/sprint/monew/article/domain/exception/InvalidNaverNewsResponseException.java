package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class InvalidNaverNewsResponseException extends ArticleException {

    public InvalidNaverNewsResponseException(String keyword) {
        super(ArticleErrorCode.INVALID_NAVER_NEWS_RESPONSE, Map.of("keyword", keyword));
    }
}

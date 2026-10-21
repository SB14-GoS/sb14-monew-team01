package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class UserIdRequiredException extends ArticleException {

    public UserIdRequiredException() {
        super(ArticleErrorCode.USER_ID_REQUIRED, Map.of());
    }
}

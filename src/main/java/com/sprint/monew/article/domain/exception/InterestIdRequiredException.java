package com.sprint.monew.article.domain.exception;

import com.sprint.monew.article.domain.ArticleErrorCode;
import java.util.Map;

public class InterestIdRequiredException extends ArticleException {

    public InterestIdRequiredException() {
        super(ArticleErrorCode.INTEREST_ID_REQUIRED, Map.of());
    }
}

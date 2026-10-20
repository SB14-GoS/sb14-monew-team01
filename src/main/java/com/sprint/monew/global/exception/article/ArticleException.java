package com.sprint.monew.global.exception.article;

import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.Map;

public class ArticleException extends MonewException {

    public ArticleException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}

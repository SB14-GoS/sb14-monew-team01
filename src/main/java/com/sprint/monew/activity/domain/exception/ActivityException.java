package com.sprint.monew.activity.domain.exception;

import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.Map;

public class ActivityException extends MonewException {

    public ActivityException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}

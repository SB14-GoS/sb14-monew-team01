package com.sprint.monew.global.exception.interest;

import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.Map;

public class InterestException extends MonewException {

    public InterestException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}

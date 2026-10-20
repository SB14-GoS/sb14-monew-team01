package com.sprint.monew.global.exception.user;

import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.Map;

public class UserException extends MonewException {

    public UserException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}

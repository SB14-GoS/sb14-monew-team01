package com.sprint.monew.backup.domain.exception;

import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.Map;

public class BackupException extends MonewException {

    public BackupException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }

    public BackupException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        super(errorCode, details);
        initCause(cause);
    }
}

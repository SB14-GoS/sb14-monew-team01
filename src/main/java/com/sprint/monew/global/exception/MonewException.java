package com.sprint.monew.global.exception;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true)
public class MonewException extends RuntimeException {
    Instant timestamp;
    ErrorCode errorCode;
    Map<String, Object> details;

    public MonewException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
        this.details = details;
    }
}

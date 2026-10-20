package com.sprint.monew.global.exception;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();

    Level getLevel();
}

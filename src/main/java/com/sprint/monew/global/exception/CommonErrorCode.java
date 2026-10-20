package com.sprint.monew.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INCORRECT_REQUEST(Level.WARN, HttpStatus.BAD_REQUEST, "올바른 입력이 아닙니다."),
    INTERNAL_SERVER_ERROR(Level.ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "예상하지 못한 서버 오류가 발생했습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

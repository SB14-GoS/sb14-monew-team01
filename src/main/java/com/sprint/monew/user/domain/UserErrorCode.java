package com.sprint.monew.user.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

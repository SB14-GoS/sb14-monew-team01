package com.sprint.monew.activity.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ActivityErrorCode implements ErrorCode {
    USER_ACTIVITY_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "해당 유저의 활동을 찾지 못 했습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

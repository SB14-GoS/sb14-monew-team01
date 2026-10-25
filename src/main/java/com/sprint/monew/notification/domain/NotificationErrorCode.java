package com.sprint.monew.notification.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "해당되는 알림을 찾지 못 했습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

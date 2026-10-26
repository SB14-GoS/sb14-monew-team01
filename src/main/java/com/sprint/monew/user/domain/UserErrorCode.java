package com.sprint.monew.user.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    EMAIL_DUPLICATED(Level.WARN, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(Level.WARN, HttpStatus.UNAUTHORIZED, " 이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_UPDATE_FORBIDDEN(Level.WARN, HttpStatus.FORBIDDEN, "사용자 정보를 수정할 권한이 없습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

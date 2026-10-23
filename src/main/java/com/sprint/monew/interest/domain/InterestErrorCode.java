package com.sprint.monew.interest.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {
    SIMILAR_NAME_EXISTS(Level.WARN, HttpStatus.CONFLICT, "유사한 이름의 관심사가 이미 존재합니다."),
    INTEREST_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "관심사를 찾을 수 없습니다."),
    ALREADY_SUBSCRIBED(Level.WARN, HttpStatus.CONFLICT, "이미 구독 중인 관심사입니다."),
    SUBSCRIPTION_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    INVALID_INTEREST_ORDER_BY(Level.WARN, HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 기준입니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

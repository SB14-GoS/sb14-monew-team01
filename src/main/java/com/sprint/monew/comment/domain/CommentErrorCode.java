package com.sprint.monew.comment.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    ARTICLE_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다."),
    USER_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

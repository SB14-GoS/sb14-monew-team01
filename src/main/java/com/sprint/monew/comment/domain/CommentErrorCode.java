package com.sprint.monew.comment.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    EXAMPLE(null, null, null);

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

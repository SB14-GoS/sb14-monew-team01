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
    USER_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    ARTICLE_ID_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 ID는 필수입니다."),
    USER_ID_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다."),
    INVALID_COMMENT_CONTENT(Level.WARN, HttpStatus.BAD_REQUEST, "댓글 내용은 공백이 아닌 1~500자여야 합니다."),
    INVALID_COMMENT_ORDER_BY(Level.WARN, HttpStatus.BAD_REQUEST, "지원하지 않는 댓글 정렬 기준입니다."),
    INVALID_COMMENT_CURSOR(Level.WARN, HttpStatus.BAD_REQUEST, "유효하지 않은 댓글 커서입니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

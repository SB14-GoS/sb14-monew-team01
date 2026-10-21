package com.sprint.monew.article.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode implements ErrorCode {
    INVALID_ARTICLE_TITLE(Level.WARN, HttpStatus.BAD_REQUEST, "기사 제목은 공백이 아닌 1~255자여야 합니다."),
    ARTICLE_PUBLISHED_DATE_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 발행일은 필수입니다."),
    ARTICLE_SOURCE_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 출처는 필수입니다."),
    ARTICLE_SOURCE_URL_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 원본 URL은 필수입니다."),
    ARTICLE_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "연결할 기사는 필수입니다."),
    INTEREST_ID_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "연결할 관심사 ID는 필수입니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

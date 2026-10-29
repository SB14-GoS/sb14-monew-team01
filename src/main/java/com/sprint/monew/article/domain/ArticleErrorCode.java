package com.sprint.monew.article.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode implements ErrorCode {
    ARTICLE_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "찾고자 하는 기사가 없습니다."),
    INVALID_NAVER_NEWS_RESPONSE(Level.ERROR, HttpStatus.BAD_GATEWAY, "네이버 뉴스 응답에 기사 목록이 없습니다."),
    USER_ID_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다."),
    INVALID_ARTICLE_TITLE(Level.WARN, HttpStatus.BAD_REQUEST, "기사 제목은 공백이 아닌 1~255자여야 합니다."),
    ARTICLE_PUBLISHED_DATE_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 발행일은 필수입니다."),
    ARTICLE_SOURCE_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 출처는 필수입니다."),
    ARTICLE_SOURCE_URL_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "기사 원본 URL은 필수입니다."),
    ARTICLE_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "연결할 기사는 필수입니다."),
    INTEREST_ID_REQUIRED(Level.WARN, HttpStatus.BAD_REQUEST, "연결할 관심사 ID는 필수입니다."),
    INVALID_RSS_RESPONSE(Level.ERROR, HttpStatus.BAD_GATEWAY, "RSS 연결중 문제가 발생했습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

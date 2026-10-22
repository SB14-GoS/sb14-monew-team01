package com.sprint.monew.article.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArticleOrderBy {
    PUBLISH_DATE("publishDate"),
    COMMENT_COUNT("commentCount"),
    VIEW_COUNT("viewCount");

    @JsonValue
    private final String value;
}

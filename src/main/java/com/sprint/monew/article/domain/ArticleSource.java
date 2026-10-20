package com.sprint.monew.article.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum ArticleSource {
    NAVER,
    HANKYUNG,
    CHOSUN,
    YEONHAP
}

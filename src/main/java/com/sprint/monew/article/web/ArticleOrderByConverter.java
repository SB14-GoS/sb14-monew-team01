package com.sprint.monew.article.web;

import com.sprint.monew.article.domain.ArticleOrderBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ArticleOrderByConverter implements Converter<String, ArticleOrderBy> {

    @Override
    public ArticleOrderBy convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        for (ArticleOrderBy orderBy : ArticleOrderBy.values()) {
            if (orderBy.getValue().equals(source)) {
                return orderBy;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 기사 정렬 기준: " + source);
    }
}

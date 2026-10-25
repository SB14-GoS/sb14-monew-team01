package com.sprint.monew.article.web.dto;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleDto(
        UUID id,
        ArticleSource source,
        String sourceUrl,
        String title,
        Instant publishDate,
        String summary,
        long commentCount,
        long viewCount,
        boolean viewedByMe) {

    public static ArticleDto from(Article article, boolean viewedByMe) {
        return new ArticleDto(
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedDate(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount(),
                viewedByMe);
    }
}

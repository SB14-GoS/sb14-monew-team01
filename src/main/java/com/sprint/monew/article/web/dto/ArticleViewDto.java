package com.sprint.monew.article.web.dto;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleView;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewDto(
        // 명세서와 일치하기 위한 네이밍
        UUID id, // articleView id
        UUID viewedBy, // userId
        Instant createdAt, // 조회 시각
        UUID articleId,
        String source,
        String sourceUrl,
        String articleTitle,
        Instant articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount) {

    public static ArticleViewDto from(ArticleView view, Article article, long viewCount) {
        return new ArticleViewDto(
                view.getId(),
                view.getUserId(),
                view.getCreatedAt(),
                article.getId(),
                article.getSource().name(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedDate(),
                article.getSummary(),
                article.getCommentCount(),
                viewCount);
    }
}

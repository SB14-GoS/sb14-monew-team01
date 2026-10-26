package com.sprint.monew.activity.web.dto;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewActivityDto(
        UUID id,
        UUID viewedBy,
        Instant createdAt,
        UUID articleId,
        String source,
        String sourceUrl,
        String articleTitle,
        Instant articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount) {

    public static ArticleViewActivityDto from(ArticleViewItem item) {
        return new ArticleViewActivityDto(
                item.articleViewId(),
                item.viewedBy(),
                item.createdAt(),
                item.articleId(),
                item.source(),
                item.sourceUrl(),
                item.articleTitle(),
                item.articlePublishedDate(),
                item.articleSummary(),
                item.articleCommentCount(),
                item.articleViewCount());
    }
}

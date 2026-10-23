package com.sprint.monew.article.api;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewedEvent(
        UUID articleViewId,
        UUID userId,
        Instant viewedAt,
        UUID articleId,
        String source,
        String sourceUrl,
        String articleTitle,
        Instant articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount) {}

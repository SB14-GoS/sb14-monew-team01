package com.sprint.monew.activity.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewActivityDto(
        UUID id,
        UUID viewedBy,
        Integer createdAt,
        UUID articleId,
        String source,
        String sourceUrl,
        String articleTitle,
        Instant articlePublishedDate,
        String articleSummary,
        int articleCommentCount,
        int articleViewCount) {}

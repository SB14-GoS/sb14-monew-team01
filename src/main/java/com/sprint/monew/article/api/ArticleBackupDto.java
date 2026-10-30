package com.sprint.monew.article.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleBackupDto(
        UUID id,
        List<UUID> interestIds,
        String title,
        Instant publishedDate,
        String summary,
        String source,
        String sourceUrl) {}

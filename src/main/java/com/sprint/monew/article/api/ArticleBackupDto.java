package com.sprint.monew.article.api;

import java.time.Instant;
import java.util.UUID;

public record ArticleBackupDto(
        UUID id, String title, Instant publishedDate, String summary, String source, String sourceUrl) {}

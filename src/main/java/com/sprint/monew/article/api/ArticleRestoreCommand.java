package com.sprint.monew.article.api;

import java.time.Instant;

public record ArticleRestoreCommand(
        String title, Instant publishedDate, String summary, String source, String sourceUrl) {}

package com.sprint.monew.article.domain.repository;

import java.time.Instant;
import java.util.UUID;

public record ArticleSearchCursor(Instant publishDate, Long count, Instant after, UUID articleId) {}

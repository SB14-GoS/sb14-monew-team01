package com.sprint.monew.article.application.port;

import com.sprint.monew.article.domain.ArticleSource;
import java.time.Instant;

public record RawArticle(
        ArticleSource articleSource, String sourceUrl, String title, Instant publishDate, String summary) {}

package com.sprint.monew.article.infra.source.dto;

import java.util.List;

public record NaverNewsResponse(
        String lastBuildDate, Integer total, Integer start, Integer display, List<NaverNewsItem> items) {}

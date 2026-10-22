package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleSearchCondition(
        String keyword,
        UUID interestId,
        List<ArticleSource> sourceIn,
        LocalDateTime publishDateFrom,
        LocalDateTime publishDateTo,
        String orderBy,
        SortDirection direction,
        CursorRequest page) {}

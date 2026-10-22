package com.sprint.monew.article.web.dto;

import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public record ArticleSearchRequest(
        String keyword,
        UUID interestId,
        List<ArticleSource> sourceIn,
        @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime publishDateFrom,
        @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime publishDateTo,
        String orderBy,
        SortDirection direction,
        String cursor,
        @DateTimeFormat(iso = ISO.DATE_TIME) Instant after,
        @Min(1) @Max(100) Integer limit) {

    public ArticleSearchRequest {
        orderBy = orderBy == null || orderBy.isEmpty() ? "publishDate" : orderBy; // 기본 게시ㅡ
        direction = direction == null ? SortDirection.DESC : direction; // 기본 내림차순
        limit = limit == null ? 10 : limit; // 기본 10개
    }

    public ArticleSearchCondition toCondition() {
        return new ArticleSearchCondition(
                keyword,
                interestId,
                sourceIn,
                publishDateFrom,
                publishDateTo,
                orderBy,
                direction,
                new CursorRequest(cursor, after, limit));
    }
}

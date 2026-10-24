package com.sprint.monew.interest.web.dto;

import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.interest.domain.InterestSearchCondition;
import com.sprint.monew.interest.domain.InterestSearchCursor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public record InterestSearchRequest(
        String keyword,
        @NotBlank(message = "정렬 기준은 필수입니다.") String orderBy,
        @NotNull(message = "정렬 방향은 필수입니다.") SortDirection direction,
        String cursor,
        @DateTimeFormat(iso = ISO.DATE_TIME) Instant after,

        @NotNull(message = "페이지 크기는 필수입니다.") @Min(1) @Max(100)
        Integer limit) {

    public InterestSearchCondition toCondition() {
        return new InterestSearchCondition(keyword);
    }

    public InterestSearchCursor toCursor() {
        return new InterestSearchCursor(cursor, after, limit, orderBy, direction);
    }
}

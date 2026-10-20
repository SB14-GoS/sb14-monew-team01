package com.sprint.monew.global.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public record CursorRequest(
        String cursor,
        @DateTimeFormat(iso = ISO.DATE_TIME) Instant after,
        @NotNull @Min(1) Integer limit) {}

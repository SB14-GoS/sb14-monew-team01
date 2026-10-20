package com.sprint.monew.global.common;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content, String nextCursor, Instant nextAfter, int size, long totalElements, boolean hasNext) {

    public static <T> CursorPageResponse<T> of(
            List<T> fetched,
            int limit,
            long totalElements,
            Function<T, String> cursorOf,
            Function<T, Instant> afterOf) {
        boolean hasNext = fetched.size() > limit;
        List<T> content = List.copyOf(hasNext ? fetched.subList(0, limit) : fetched);
        T last = (hasNext && !content.isEmpty()) ? content.get(content.size() - 1) : null;
        return new CursorPageResponse<>(
                content,
                last == null ? null : cursorOf.apply(last),
                last == null ? null : afterOf.apply(last),
                content.size(),
                totalElements,
                hasNext);
    }
}

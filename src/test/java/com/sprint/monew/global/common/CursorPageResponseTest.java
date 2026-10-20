package com.sprint.monew.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorPageResponseTest {

    record Item(String name, Instant createdAt) {}

    private static final Instant BASE = Instant.parse("2026-10-20T00:00:00Z");

    private static List<Item> items(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Item("item" + i, BASE.plusSeconds(i)))
                .toList();
    }

    private static CursorPageResponse<Item> page(List<Item> fetched, int limit, long totalElements) {
        return CursorPageResponse.of(fetched, limit, totalElements, Item::name, Item::createdAt);
    }

    @Test
    @DisplayName("조회 결과가 limit보다 적으면 다음 페이지가 없고 커서는 null이다")
    void lessThanLimit() {
        CursorPageResponse<Item> response = page(items(3), 5, 3);

        assertThat(response.content()).hasSize(3);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextAfter()).isNull();
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("조회 결과가 정확히 limit개면 다음 페이지가 없다")
    void exactlyLimit() {
        CursorPageResponse<Item> response = page(items(5), 5, 5);

        assertThat(response.content()).hasSize(5);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextAfter()).isNull();
    }

    @Test
    @DisplayName("조회 결과가 limit+1개면 limit개만 담고 마지막 요소로 다음 커서를 만든다")
    void moreThanLimit() {
        List<Item> fetched = items(6);

        CursorPageResponse<Item> response = page(fetched, 5, 20);

        Item lastInPage = fetched.get(4);
        assertThat(response.content()).hasSize(5).doesNotContain(fetched.get(5));
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(lastInPage.name());
        assertThat(response.nextAfter()).isEqualTo(lastInPage.createdAt());
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(20);
    }

    @Test
    @DisplayName("조회 결과가 비어 있으면 빈 페이지를 반환한다")
    void empty() {
        CursorPageResponse<Item> response = page(List.of(), 5, 0);

        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextAfter()).isNull();
        assertThat(response.size()).isZero();
        assertThat(response.totalElements()).isZero();
    }
}

package com.sprint.monew.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseSoftDeleteEntityTest {

    static class SampleEntity extends BaseSoftDeleteEntity {}

    @Test
    @DisplayName("논리 삭제하면 삭제 시각이 기록된다")
    void softDelete() {
        SampleEntity entity = new SampleEntity();

        entity.softDelete();

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 삭제된 엔티티를 다시 삭제해도 처음 삭제 시각을 유지한다")
    void softDeleteTwice() {
        SampleEntity entity = new SampleEntity();
        entity.softDelete();
        Instant firstDeletedAt = entity.getDeletedAt();

        entity.softDelete();

        assertThat(entity.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("복구하면 삭제 상태가 해제된다")
    void restore() {
        SampleEntity entity = new SampleEntity();
        entity.softDelete();

        entity.restore();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }
}

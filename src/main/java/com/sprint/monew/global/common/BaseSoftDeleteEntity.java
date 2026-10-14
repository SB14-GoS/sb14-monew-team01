package com.sprint.monew.global.common;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 논리 삭제를 지원하는 엔티티의 공통 부모.
 *
 * <p>논리 삭제는 행을 지우지 않고 {@code deletedAt}에 삭제 시각을 기록한다. 조회 시 논리 삭제된 데이터를 제외하는 조건은 각
 * Repository(쿼리)에서 {@code deletedAt is null}로 직접 건다.
 *
 * <pre>{@code
 * @Entity
 * public class Article extends BaseSoftDeleteEntity { ... }
 *
 * article.softDelete();   // 논리 삭제
 * repository.delete(article);   // 물리 삭제
 * }</pre>
 */
@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseSoftDeleteEntity extends BaseEntity {

    private Instant deletedAt;

    /** 논리 삭제한다. 이미 삭제된 경우 처음 삭제 시각을 유지한다. */
    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }

    /** 논리 삭제를 취소한다. */
    public void restore() {
        deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

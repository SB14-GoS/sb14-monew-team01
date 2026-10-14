package com.sprint.monew.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class BaseEntityAuditingTest {

    @Entity(name = "audit_sample")
    static class AuditSample extends BaseSoftDeleteEntity {}

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("저장하면 UUID 식별자와 생성·수정 시각이 자동으로 채워진다")
    void auditingOnPersist() {
        AuditSample sample = new AuditSample();

        em.persist(sample);
        em.flush();

        assertThat(sample.getId()).isNotNull();
        assertThat(sample.getCreatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(sample.getUpdatedAt()).isNotNull();
        assertThat(sample.getDeletedAt()).isNull();
    }
}

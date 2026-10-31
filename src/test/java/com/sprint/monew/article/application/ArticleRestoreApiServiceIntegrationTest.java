package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.api.ArticleRestoreApi;
import com.sprint.monew.article.api.ArticleRestoreCommand;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.interest.domain.Interest;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleRestoreApiServiceIntegrationTest {

    @Autowired
    ArticleRestoreApi articleRestoreApi;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("복구하면 살아 있는 관심사 매핑만 중복 없이 다시 만든다")
    void restoreRecreatesExistingInterestMappings() {
        Interest interest = new Interest("복구 관심사 " + UUID.randomUUID(), List.of("반도체"));
        em.persist(interest);
        UUID deletedInterestId = UUID.randomUUID();
        String sourceUrl = "https://example.com/" + UUID.randomUUID();
        ArticleRestoreCommand command = new ArticleRestoreCommand(
                "복구 기사",
                Instant.parse("2026-10-28T01:00:00Z"),
                "요약",
                "NAVER",
                sourceUrl,
                List.of(interest.getId(), interest.getId(), deletedInterestId));

        List<UUID> restoredIds = articleRestoreApi.restore(List.of(command));
        em.flush();
        em.clear();

        assertThat(restoredIds).hasSize(1);
        Article restored = em.find(Article.class, restoredIds.get(0));
        assertThat(restored.getSourceUrl()).isEqualTo(sourceUrl);
        List<UUID> mappedInterestIds = em.createQuery(
                        "select ai.interestId from ArticleInterest ai where ai.article.id = :articleId", UUID.class)
                .setParameter("articleId", restored.getId())
                .getResultList();
        assertThat(mappedInterestIds).containsExactly(interest.getId());
    }

    @Test
    @DisplayName("소프트 삭제된 기사의 sourceUrl도 이미 있는 것으로 본다")
    void findExistingSourceUrlsIncludesSoftDeleted() {
        Article softDeleted = Article.create(
                "삭제된 기사",
                Instant.parse("2026-10-28T01:00:00Z"),
                "요약",
                ArticleSource.NAVER,
                "https://example.com/" + UUID.randomUUID());
        softDeleted.softDelete();
        em.persist(softDeleted);
        em.flush();
        String missingUrl = "https://example.com/" + UUID.randomUUID();

        assertThat(articleRestoreApi.findExistingSourceUrls(List.of(softDeleted.getSourceUrl(), missingUrl)))
                .containsExactly(softDeleted.getSourceUrl());
    }
}

package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.api.ArticleBackupApi;
import com.sprint.monew.article.api.ArticleBackupDto;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleBackupApiServiceIntegrationTest {

    @Autowired
    ArticleBackupApi articleBackupApi;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("기간 안에 생성된 기사를 관심사 id와 함께 돌려주고, 매핑이 없으면 빈 목록, 논리 삭제 기사는 뺀다")
    void findAllCreatedBetweenGroupsInterestIds() {
        Article mapped = article();
        Article unmapped = article();
        Article softDeleted = article();
        softDeleted.softDelete();
        em.persist(mapped);
        em.persist(unmapped);
        em.persist(softDeleted);
        UUID firstInterestId = UUID.randomUUID();
        UUID secondInterestId = UUID.randomUUID();
        em.persist(ArticleInterest.create(mapped, firstInterestId));
        em.persist(ArticleInterest.create(mapped, secondInterestId));
        em.flush();
        Instant now = Instant.now();

        List<ArticleBackupDto> result =
                articleBackupApi.findAllCreatedBetween(now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(1)));

        List<UUID> ids = List.of(mapped.getId(), unmapped.getId(), softDeleted.getId());
        List<ArticleBackupDto> ours =
                result.stream().filter(dto -> ids.contains(dto.id())).toList();
        assertThat(ours).extracting(ArticleBackupDto::id).containsExactlyInAnyOrder(mapped.getId(), unmapped.getId());
        assertThat(ours)
                .filteredOn(dto -> dto.id().equals(mapped.getId()))
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.interestIds()).containsExactlyInAnyOrder(firstInterestId, secondInterestId);
                    assertThat(dto.source()).isEqualTo("NAVER");
                    assertThat(dto.sourceUrl()).isEqualTo(mapped.getSourceUrl());
                });
        assertThat(ours)
                .filteredOn(dto -> dto.id().equals(unmapped.getId()))
                .singleElement()
                .satisfies(dto -> assertThat(dto.interestIds()).isEmpty());
    }

    @Test
    @DisplayName("기간 안에 생성된 기사가 없으면 빈 목록을 돌려준다")
    void findAllCreatedBetweenReturnsEmpty() {
        Instant past = Instant.parse("2000-01-01T00:00:00Z");

        assertThat(articleBackupApi.findAllCreatedBetween(past, past.plus(Duration.ofDays(1))))
                .isEmpty();
    }

    private Article article() {
        return Article.create(
                "백업 테스트 기사",
                Instant.parse("2026-10-28T01:00:00Z"),
                "요약",
                ArticleSource.NAVER,
                "https://example.com/" + UUID.randomUUID());
    }
}

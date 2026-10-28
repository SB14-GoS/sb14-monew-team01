package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.ArticleView;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleDeletionIntegrationTest {

    @Autowired
    ArticleService articleService;

    @Autowired
    EntityManager em;

    @Test
    void hardDeleteRemovesOnlyTargetArticleAndItsViewsAndInterestMappings() {
        Article target = saveArticle();
        target.softDelete();
        Article other = saveArticle();
        List<ArticleView> targetViews =
                List.of(ArticleView.create(target, UUID.randomUUID()), ArticleView.create(target, UUID.randomUUID()));
        List<ArticleInterest> targetInterests = List.of(
                ArticleInterest.create(target, UUID.randomUUID()), ArticleInterest.create(target, UUID.randomUUID()));
        targetViews.forEach(em::persist);
        targetInterests.forEach(em::persist);
        ArticleView otherView = ArticleView.create(other, UUID.randomUUID());
        ArticleInterest otherInterest = ArticleInterest.create(other, UUID.randomUUID());
        em.persist(otherView);
        em.persist(otherInterest);
        em.flush();
        em.clear();

        articleService.deleteHard(target.getId());
        em.flush();
        em.clear();

        assertThat(em.find(Article.class, target.getId())).isNull();
        targetViews.forEach(
                view -> assertThat(em.find(ArticleView.class, view.getId())).isNull());
        targetInterests.forEach(interest ->
                assertThat(em.find(ArticleInterest.class, interest.getId())).isNull());
        assertThat(em.find(Article.class, other.getId())).isNotNull();
        assertThat(em.find(ArticleView.class, otherView.getId())).isNotNull();
        assertThat(em.find(ArticleInterest.class, otherInterest.getId())).isNotNull();
    }

    private Article saveArticle() {
        Article article = Article.create(
                "삭제 테스트 기사",
                Instant.parse("2026-10-26T00:00:00Z"),
                "요약",
                ArticleSource.NAVER,
                "https://example.com/" + UUID.randomUUID());
        em.persist(article);
        return article;
    }
}

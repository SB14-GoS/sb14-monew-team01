package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleCommentCountIntegrationTest {

    @Autowired
    ArticleService articleService;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    EntityManager em;

    @Test
    void lockedLookupExcludesDeletedArticles() {
        Article active = saveArticle(1);
        Article deleted = saveArticle(1);
        deleted.softDelete();
        em.flush();
        em.clear();

        assertThat(articleRepository.findWithLockByIdAndDeletedAtIsNull(active.getId()))
                .isPresent();
        assertThat(articleRepository.findWithLockByIdAndDeletedAtIsNull(deleted.getId()))
                .isEmpty();
        assertThat(articleRepository.findWithLockByIdAndDeletedAtIsNull(UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void decreaseQuerySkipsZeroAndDeletedArticles() {
        Article zero = saveArticle(0);
        Article deleted = saveArticle(2);
        deleted.softDelete();
        em.flush();
        em.clear();

        assertThat(articleRepository.decreaseCommentCount(zero.getId())).isZero();
        assertThat(articleRepository.decreaseCommentCount(deleted.getId())).isZero();
        assertThat(em.find(Article.class, zero.getId()).getCommentCount()).isZero();
        assertThat(em.find(Article.class, deleted.getId()).getCommentCount()).isEqualTo(2);
    }

    @Test
    void stalePositiveCountDoesNotMakeDatabaseCountNegativeOrThrowNotFound() {
        Article article = saveArticle(1);
        em.flush();
        // 영속성 컨텍스트의 1은 유지하고 DB 값만 0으로 만들어 오래된 조회 상태를 재현한다.
        em.createQuery("update Article a set a.commentCount = 0 where a.id = :articleId")
                .setParameter("articleId", article.getId())
                .executeUpdate();
        assertThat(article.getCommentCount()).isEqualTo(1);

        articleService.decreaseCommentCount(article.getId());

        em.flush();
        em.clear();
        assertThat(em.find(Article.class, article.getId()).getCommentCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 3})
    void decreasesPositiveCountAndPreservesZero(long count) {
        Article article = saveArticle(count);
        em.flush();
        em.clear();

        articleService.decreaseCommentCount(article.getId());

        em.flush();
        em.clear();
        assertThat(em.find(Article.class, article.getId()).getCommentCount()).isEqualTo(Math.max(0, count - 1));
    }

    private Article saveArticle(long count) {
        Article article = Article.create(
                "댓글 수 테스트 기사",
                Instant.parse("2026-10-27T00:00:00Z"),
                "요약",
                ArticleSource.NAVER,
                "https://example.com/" + UUID.randomUUID());
        ReflectionTestUtils.setField(article, "commentCount", count);
        em.persist(article);
        return article;
    }
}

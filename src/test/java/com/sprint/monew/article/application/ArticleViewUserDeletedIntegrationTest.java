package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.ArticleView;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.api.UserDeletedEvent;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ArticleViewUserDeletedIntegrationTest {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("사용자가 삭제되면 그 사용자의 조회 기록만 지우고 조회 수는 유지한다")
    void userDeletedRemovesOnlyTheirViews() {
        UUID deletedUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Article first = saveArticle();
        Article second = saveArticle();
        List<ArticleView> deletedUserViews =
                List.of(ArticleView.create(first, deletedUserId), ArticleView.create(second, deletedUserId));
        ArticleView otherUserView = ArticleView.create(first, otherUserId);
        deletedUserViews.forEach(em::persist);
        em.persist(otherUserView);
        articleRepository.increaseViewCount(first.getId());
        articleRepository.increaseViewCount(first.getId());

        eventPublisher.publishEvent(new UserDeletedEvent(deletedUserId));
        em.flush();
        em.clear();

        deletedUserViews.forEach(
                view -> assertThat(em.find(ArticleView.class, view.getId())).isNull());
        assertThat(em.find(ArticleView.class, otherUserView.getId())).isNotNull();
        assertThat(em.find(Article.class, first.getId()).getViewCount()).isEqualTo(2);
    }

    private Article saveArticle() {
        Article article = Article.create(
                "조회 기록 삭제 테스트 기사",
                Instant.parse("2026-10-29T00:00:00Z"),
                "요약",
                ArticleSource.NAVER,
                "https://example.com/" + UUID.randomUUID());
        em.persist(article);
        return article;
    }
}

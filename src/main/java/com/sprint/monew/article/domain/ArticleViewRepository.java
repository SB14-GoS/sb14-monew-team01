package com.sprint.monew.article.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

    Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);
}

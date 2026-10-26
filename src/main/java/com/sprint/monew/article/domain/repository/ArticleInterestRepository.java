package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.ArticleInterest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {
    @Modifying
    @Query("delete from ArticleInterest a where a.article.id = :articleId")
    void deleteArticleInterestsByArticleId(@Param("articleId") UUID articleId);
}

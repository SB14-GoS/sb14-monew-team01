package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.Article;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :articleId")
    int increaseViewCount(@Param("articleId") UUID articleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Article a set a.commentCount = a.commentCount + 1 where a.id = :articleId")
    int increaseCommentCount(@Param("articleId") UUID articleId);
}

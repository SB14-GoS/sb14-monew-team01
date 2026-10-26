package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.ArticleView;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

    Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);

    /* 아티클 뷰에서 입력받은 userId와 동일하면서, 입력받은 articleIds이 포함된 행들에서 아티클 아이디만 가져와라 */
    @Query("select v.article.id from ArticleView v where v.userId = :userId and v.article.id in :articleIds")
    Set<UUID> findViewedArticleIds(@Param("userId") UUID userId, @Param("articleIds") Collection<UUID> articleIds);

    @Modifying
    @Query("delete from ArticleView a where a.article.id = :articleId")
    void deleteArticleViewsByArticleId(@Param("articleId") UUID articleId);
}

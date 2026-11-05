package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

    @Query("""
        select case when count(a) > 0 then true else false end
        from ArticleInterest a
        where a.article.id = :articleId and a.interestId = :interestId
        """)
    boolean existsMapping(@Param("articleId") UUID articleId, @Param("interestId") UUID interestId);

    @Modifying
    @Query("delete from ArticleInterest a where a.article.id = :articleId")
    void deleteArticleInterestsByArticleId(@Param("articleId") UUID articleId);

    @Query("""
        select new com.sprint.monew.article.domain.repository.ArticleInterestIdPair(ai.article.id, ai.interestId)
        from ArticleInterest ai
        where ai.article.id in :articleIds
        """)
    List<ArticleInterestIdPair> findInterestIdPairsByArticleIds(@Param("articleIds") List<UUID> articleIds);
}

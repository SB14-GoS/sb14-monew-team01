package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.Article;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Article> findWithLockById(UUID articleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :articleId")
    int increaseViewCount(@Param("articleId") UUID articleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Article a set a.commentCount = a.commentCount + 1 "
            + "where a.id = :articleId and a.deletedAt is null")
    int increaseCommentCount(@Param("articleId") UUID articleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Article a set a.commentCount = a.commentCount - 1 "
            + "where a.id = :articleId and a.commentCount > 0")
    int decreaseCommentCount(@Param("articleId") UUID articleId);

    @Query("select a from Article a where a.createdAt >= :from and a.createdAt < :to and a.deletedAt is null")
    List<Article> findByCreatedAtRange(@Param("from") Instant from, @Param("to") Instant to);

    List<Article> findAllBySourceUrlIn(List<String> sourceUrls);

    @Query("""
            select max(a.publishedDate) from Article a
            where a.source = com.sprint.monew.article.domain.ArticleSource.NAVER
              and (locate(lower(:keyword), lower(a.title)) > 0
                   or locate(lower(:keyword), lower(coalesce(a.summary, ''))) > 0)
            """)
    Optional<Instant> findLatestNaverPublishedDate(@Param("keyword") String keyword);

    @Query("select a.sourceUrl from Article a where a.sourceUrl in :sourceUrls")
    List<String> findSourceUrlsIn(@Param("sourceUrls") List<String> sourceUrls);
}

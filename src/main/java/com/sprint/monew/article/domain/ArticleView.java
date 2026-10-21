package com.sprint.monew.article.domain;

import com.sprint.monew.article.domain.exception.ArticleRequiredException;
import com.sprint.monew.article.domain.exception.UserIdRequiredException;
import com.sprint.monew.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "article_views",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_article_view_article_user",
                        columnNames = {"article_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleView extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private ArticleView(Article article, UUID userId) {
        if (article == null) {
            throw new ArticleRequiredException();
        }
        if (userId == null) {
            throw new UserIdRequiredException();
        }
        this.article = article;
        this.userId = userId;
    }

    public static ArticleView create(Article article, UUID userId) {
        return new ArticleView(article, userId);
    }
}

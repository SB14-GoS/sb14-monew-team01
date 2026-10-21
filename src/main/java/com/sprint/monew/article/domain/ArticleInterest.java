package com.sprint.monew.article.domain;

import com.sprint.monew.article.domain.exception.ArticleRequiredException;
import com.sprint.monew.article.domain.exception.InterestIdRequiredException;
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
import lombok.experimental.FieldDefaults;

@Entity
@Getter
/* 아티클하고 관심사는 중복된 조합이 있으면 안됌 A + B 조합은 하나만 존재 */
@Table(
        name = "article_interests",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_article_interest",
                        columnNames = {"article_id", "interest_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArticleInterest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    Article article;

    @Column(name = "interest_id", nullable = false)
    UUID interestId;

    private ArticleInterest(Article article, UUID interestId) {
        if (article == null) {
            throw new ArticleRequiredException();
        }
        if (interestId == null) {
            throw new InterestIdRequiredException();
        }

        this.article = article;
        this.interestId = interestId;
    }

    public static ArticleInterest create(Article article, UUID interestId) {
        return new ArticleInterest(article, interestId);
    }
}

package com.sprint.monew.article.domain;

import com.sprint.monew.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Table(name = "article")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Article extends BaseSoftDeleteEntity {

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    Instant publishedDate;

    @Column(columnDefinition = "text") // 255 범위 상한 해제
    String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ArticleSource source;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    String sourceUrl;

    /* 생성시 조회수와 댓글은 항상 0이기에 */
    @Column(nullable = false)
    long commentCount = 0;

    @Column(nullable = false)
    long viewCount = 0;

    private Article(String title, Instant publishedDate, String summary, ArticleSource source, String sourceUrl) {
        this.title = title;
        this.publishedDate = Objects.requireNonNull(publishedDate);
        this.summary = summary;
        this.source = Objects.requireNonNull(source);
        this.sourceUrl = sourceUrl;
    }

    public static Article create(
            String title, Instant publishedDate, String summary, ArticleSource source, String sourceUrl) {
        return new Article(title, publishedDate, summary, source, sourceUrl);
    }
}

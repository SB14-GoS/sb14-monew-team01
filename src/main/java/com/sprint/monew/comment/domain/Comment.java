package com.sprint.monew.comment.domain;

import com.sprint.monew.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@NoArgsConstructor
@Getter
public class Comment extends BaseSoftDeleteEntity {

    @Column(nullable = false)
    private UUID articleId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long likeCount;

    @OneToMany(
            mappedBy = "comment",
            cascade = {CascadeType.REMOVE},
            fetch = FetchType.LAZY)
    private List<CommentLike> commentLikes = new ArrayList<>();

    public Comment(UUID articleId, UUID userId, String content) {
        this.articleId = articleId;
        this.userId = userId;
        this.content = content;
    }
}

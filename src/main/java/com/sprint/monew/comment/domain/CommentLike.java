package com.sprint.monew.comment.domain;

import com.sprint.monew.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "comment_like",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_comment_like_comment_user",
                    columnNames = {"comment_id", "user_id"})
        })
public class CommentLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(nullable = false)
    private UUID userId;

    public CommentLike(Comment comment, UUID userId) {
        this.comment = comment;
        this.userId = userId;
    }
}

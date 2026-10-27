package com.sprint.monew.comment.domain;

import com.sprint.monew.comment.domain.exception.ArticleIdRequiredException;
import com.sprint.monew.comment.domain.exception.CommentAccessDeniedException;
import com.sprint.monew.comment.domain.exception.CommentUserIdRequiredException;
import com.sprint.monew.comment.domain.exception.InvalidCommentContentException;
import com.sprint.monew.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Comment extends BaseSoftDeleteEntity {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private long likeCount = 0L;

    @OneToMany(mappedBy = "comment")
    private List<CommentLike> commentLike = new ArrayList<>();

    public Comment(UUID articleId, UUID userId, String content) {
        if (articleId == null) {
            throw new ArticleIdRequiredException();
        }
        if (userId == null) {
            throw new CommentUserIdRequiredException();
        }
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new InvalidCommentContentException(content);
        }
        this.articleId = articleId;
        this.userId = userId;
        this.content = content;
        this.likeCount = 0L;
    }

    public void updateContent(UUID userId, String content) {
        if (userId == null) {
            throw new CommentUserIdRequiredException();
        }

        if (!this.userId.equals(userId)) {
            throw new CommentAccessDeniedException(getId(), userId);
        }

        if (content == null || content.isBlank() || content.length() > 500) {
            throw new InvalidCommentContentException(content);
        }
        this.content = content;
    }

    public void increaseLikeCount() {
        likeCount++;
    }
}

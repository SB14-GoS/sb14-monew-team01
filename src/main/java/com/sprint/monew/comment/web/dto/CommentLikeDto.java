package com.sprint.monew.comment.web.dto;

import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(
        UUID id,
        UUID likedBy,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {

    public static CommentLikeDto from(CommentLike commentLike, String commentUserNickname) {
        Comment comment = commentLike.getComment();

        return new CommentLikeDto(
                commentLike.getId(),
                commentLike.getUserId(),
                commentLike.getCreatedAt(),
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                commentUserNickname,
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }
}

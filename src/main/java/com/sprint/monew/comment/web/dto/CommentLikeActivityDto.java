package com.sprint.monew.comment.web.dto;

import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeActivityDto(
        UUID id,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {
    public static CommentLikeActivityDto from(
            CommentLike commentLike, String articleTitle, String commentUserNickname) {
        Comment comment = commentLike.getComment();
        return new CommentLikeActivityDto(
                commentLike.getId(),
                commentLike.getCreatedAt(),
                comment.getId(),
                comment.getArticleId(),
                articleTitle,
                comment.getUserId(),
                commentUserNickname,
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }
}

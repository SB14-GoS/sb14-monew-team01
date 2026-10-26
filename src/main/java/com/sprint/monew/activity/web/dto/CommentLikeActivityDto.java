package com.sprint.monew.activity.web.dto;

import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
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
    public static CommentLikeActivityDto from(CommentLikeItem item) {
        return new CommentLikeActivityDto(
                item.commentLikeId(),
                item.createdAt(),
                item.commentId(),
                item.articleId(),
                item.articleTitle(),
                item.commentUserId(),
                item.commentUserNickname(),
                item.commentContent(),
                item.commentLikeCount(),
                item.commentCreatedAt());
    }
}

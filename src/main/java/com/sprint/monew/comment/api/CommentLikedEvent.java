package com.sprint.monew.comment.api;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
        UUID commentLikeId,
        Instant likedAt,
        UUID likerId,
        String likerNickname,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        long commentLikeCount,
        Instant commentCreatedAt) {}

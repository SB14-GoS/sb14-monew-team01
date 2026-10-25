package com.sprint.monew.comment.api;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        Instant createdAt) {}

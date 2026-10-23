package com.sprint.monew.activity.web.dto;

import java.util.UUID;

public record CommentLikeActivityDto(
        UUID id,
        Integer createdAt,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        int commentLikeCount,
        Integer commentCreatedAt) {}

package com.sprint.monew.activity.web.dto;

import java.util.UUID;

public record CommentActivityDto(
        UUID id,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        int likeCount,
        Integer createdAt) {}

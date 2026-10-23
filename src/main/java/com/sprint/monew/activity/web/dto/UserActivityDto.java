package com.sprint.monew.activity.web.dto;

import jakarta.validation.constraints.Max;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityDto(
        UUID id,
        String email,
        String nickname,
        Instant createdAt,
        @Max(10) List<SubscriptionActivityDto> subscriptions,
        @Max(10) List<CommentActivityDto> comments,
        @Max(10) List<CommentLikeActivityDto> commentLikes,
        @Max(10) List<ArticleViewActivityDto> articleViews) {}

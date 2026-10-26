package com.sprint.monew.activity.web.dto;

import com.sprint.monew.activity.domain.UserActivityDocument;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityDto(
        UUID id,
        String email,
        String nickname,
        Instant createdAt,
        List<SubscriptionActivityDto> subscriptions,
        List<CommentActivityDto> comments,
        List<CommentLikeActivityDto> commentLikes,
        List<ArticleViewActivityDto> articleViews) {

    public static UserActivityDto from(UserActivityDocument document) {
        return new UserActivityDto(
                document.getId(),
                document.getEmail(),
                document.getNickname(),
                document.getCreatedAt(),
                document.getSubscriptions().stream()
                        .map(SubscriptionActivityDto::from)
                        .toList(),
                document.getComments().stream().map(CommentActivityDto::from).toList(),
                document.getCommentLikes().stream()
                        .map(CommentLikeActivityDto::from)
                        .toList(),
                document.getArticleViews().stream()
                        .map(ArticleViewActivityDto::from)
                        .toList());
    }
}

package com.sprint.monew.activity.web.dto;

import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import java.time.Instant;
import java.util.UUID;

public record CommentActivityDto(
        UUID id,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        Instant createdAt) {

    public static CommentActivityDto from(CommentItem item) {
        return new CommentActivityDto(
                item.commentId(),
                item.articleId(),
                item.articleTitle(),
                item.userId(),
                item.userNickname(),
                item.content(),
                item.likeCount(),
                item.createdAt());
    }
}

package com.sprint.monew.comment.web.dto;

import com.sprint.monew.comment.domain.Comment;
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
    public static CommentActivityDto from(Comment comment, String articleTitle, String userNickname) {
        return new CommentActivityDto(
                comment.getId(),
                comment.getArticleId(),
                articleTitle,
                comment.getUserId(),
                userNickname,
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }
}

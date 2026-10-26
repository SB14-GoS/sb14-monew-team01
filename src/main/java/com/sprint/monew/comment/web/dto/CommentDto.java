package com.sprint.monew.comment.web.dto;

import com.sprint.monew.comment.domain.Comment;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID articleId,
        UUID userId,
        String userNickname,
        String content,
        long likeCount,
        boolean likedByMe,
        Instant createdAt) {
    public static CommentDto from(Comment comment, String userNickname, boolean likedByMe) {
        return new CommentDto(
                comment.getId(),
                comment.getArticleId(),
                comment.getUserId(),
                userNickname,
                comment.getContent(),
                comment.getLikeCount(),
                likedByMe,
                comment.getCreatedAt());
    }
}

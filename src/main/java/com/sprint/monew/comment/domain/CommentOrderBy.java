package com.sprint.monew.comment.domain;

import com.sprint.monew.comment.domain.exception.InvalidCommentOrderByException;

public enum CommentOrderBy {
    CREATED_AT,
    LIKE_COUNT;

    public static CommentOrderBy from(String value) {
        return switch (value) {
            case "createdAt" -> CREATED_AT;
            case "likeCount" -> LIKE_COUNT;
            default -> throw new InvalidCommentOrderByException(value);
        };
    }
}

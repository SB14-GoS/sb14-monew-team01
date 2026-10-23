package com.sprint.monew.comment.api;

import java.util.UUID;

public record CommentUnlikedEvent(UUID commentId, UUID likerId, UUID commentUserId, long commentLikeCount) {}

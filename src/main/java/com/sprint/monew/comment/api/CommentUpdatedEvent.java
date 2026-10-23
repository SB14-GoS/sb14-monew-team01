package com.sprint.monew.comment.api;

import java.util.UUID;

public record CommentUpdatedEvent(UUID commentId, UUID userId, String content) {}

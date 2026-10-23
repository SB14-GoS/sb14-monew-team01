package com.sprint.monew.comment.api;

import java.util.UUID;

public record CommentDeletedEvent(UUID commentId, UUID userId) {}

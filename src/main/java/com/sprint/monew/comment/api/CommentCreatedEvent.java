package com.sprint.monew.comment.api;

import java.util.UUID;

/** 댓글이 정상적으로 등록된 뒤 알림·활동 내역 모듈에 알리는 이벤트입니다. */
public record CommentCreatedEvent(UUID commentId, UUID articleId, UUID commentAuthorId) {}

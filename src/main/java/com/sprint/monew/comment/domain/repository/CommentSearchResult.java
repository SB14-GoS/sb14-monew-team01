package com.sprint.monew.comment.domain.repository;

import com.sprint.monew.comment.domain.Comment;

public record CommentSearchResult(Comment comment, boolean likedByMe) {}

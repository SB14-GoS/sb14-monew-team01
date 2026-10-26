package com.sprint.monew.comment.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {}

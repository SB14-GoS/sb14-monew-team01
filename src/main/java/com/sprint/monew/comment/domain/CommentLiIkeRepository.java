package com.sprint.monew.comment.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLiIkeRepository extends JpaRepository<CommentLike, UUID> {}

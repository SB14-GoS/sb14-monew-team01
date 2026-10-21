package com.sprint.monew.comment.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLIkeRepository extends JpaRepository<CommentLike, UUID> {}

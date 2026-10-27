package com.sprint.monew.comment.application;

import com.sprint.monew.comment.domain.CommentLikeRepository;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.web.dto.CommentLikeDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public CommentLikeDto register(UUID commentId, UUID requesterUserId) {}
}

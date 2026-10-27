package com.sprint.monew.comment.application;

import com.sprint.monew.comment.api.CommentCommandApi;
import com.sprint.monew.comment.domain.CommentLikeRepository;
import com.sprint.monew.comment.domain.CommentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentCleanupService implements CommentCommandApi {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Override
    public void deleteAllByArticleId(UUID articleId) {
        commentLikeRepository.deleteAllByArticleId(articleId);
        commentRepository.deleteAllByArticleId(articleId);
    }
}

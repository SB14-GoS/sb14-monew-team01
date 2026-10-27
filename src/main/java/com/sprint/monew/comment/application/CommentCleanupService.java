package com.sprint.monew.comment.application;

import com.sprint.monew.comment.api.CommentCommandApi;
import com.sprint.monew.comment.api.CommentDeletedEvent;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentCleanupService implements CommentCommandApi {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void deleteAllByArticleId(UUID articleId) {

        List<CommentDeletedEvent> deletedEvents = commentRepository.findAllByArticleId(articleId).stream()
                .filter(comment -> !comment.isDeleted())
                .map(comment -> new CommentDeletedEvent(comment.getId(), comment.getUserId()))
                .toList();

        commentLikeRepository.deleteAllByArticleId(articleId);
        commentRepository.deleteAllByArticleId(articleId);

        deletedEvents.forEach(eventPublisher::publishEvent);
    }
}

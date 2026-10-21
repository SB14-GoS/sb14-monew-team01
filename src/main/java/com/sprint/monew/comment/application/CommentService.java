package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleQueryApi articleQueryApi;
    private final ArticleCommandApi articleCommandApi;
    private final UserQueryApi userQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentDto register(UUID articleId, UUID userId, String content) {
        if (!articleQueryApi.exists(articleId)) {
            throw new CommentException(CommentErrorCode.ARTICLE_NOT_FOUND, Map.of("articleId", articleId));
        }

        UserSummary user = userQueryApi.getSummary(userId);
        Comment comment = commentRepository.save(new Comment(articleId, userId, content));
        articleCommandApi.increaseCommentCount(articleId);
        eventPublisher.publishEvent(new CommentCreatedEvent(comment.getId(), articleId, userId));

        return CommentDto.from(comment, user.nickname(), false);
    }
}

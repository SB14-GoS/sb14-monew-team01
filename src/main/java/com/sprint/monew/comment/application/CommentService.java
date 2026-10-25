package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.domain.exception.CommentArticleNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.List;
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
            throw new CommentArticleNotFoundException(articleId);
        }

        if (!userQueryApi.exists(userId)) {
            throw new CommentUserNotFoundException(userId);
        }

        UserSummary user = userQueryApi.getSummary(userId);
        Comment comment = commentRepository.save(new Comment(articleId, userId, content));
        articleCommandApi.increaseCommentCount(articleId);
        eventPublisher.publishEvent(new CommentCreatedEvent(comment.getId(), articleId, userId));

        return CommentDto.from(comment, user.nickname(), false);
    }

    private CommentCreatedEvent toCreatedEvent(Comment comment) {
        return new CommentCreatedEvent(comment.getId(), comment.getArticleId(), comment.getUserId());
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<CommentDto> search(UUID articleId, CursorRequest request) {
        if (!articleQueryApi.exists(articleId)) {
            throw new CommentArticleNotFoundException(articleId);
        }

        List<CommentDto> comments = commentRepository.findByArticleId(articleId, request).stream()
                .map(comment -> CommentDto.from(
                        comment,
                        userQueryApi.getSummary(comment.getUserId()).nickname(), // n+1 문제가 생성됨.
                        false // 임시 false
                        ))
                .toList();

        long totalCommentCount = commentRepository.countByArticleId(articleId);

        return CursorPageResponse.of(
                comments,
                request.limit(),
                totalCommentCount,
                comment -> comment.id().toString(),
                CommentDto::createdAt);
    }
}

package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.article.domain.exception.ArticleNotFoundException;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentOrderBy;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.domain.CommentSearchResult;
import com.sprint.monew.comment.domain.exception.CommentArticleNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

        // getSummary()는 탈퇴 사용자도 반환할 수 있으므로, 댓글 등록 전 활동 가능한 사용자 여부를 확인한다.
        if (!userQueryApi.exists(userId)) {
            throw new CommentUserNotFoundException(userId);
        }

        UserSummary user = userQueryApi.getSummary(userId);

        String articleTitle = getArticleTitle(articleId);

        Comment comment = commentRepository.save(new Comment(articleId, userId, content));
        articleCommandApi.increaseCommentCount(articleId);
        eventPublisher.publishEvent(toCreatedEvent(comment, articleTitle, user.nickname()));

        return CommentDto.from(comment, user.nickname(), false);
    }

    private CommentCreatedEvent toCreatedEvent(Comment comment, String articleTitle, String userNickname) {
        return new CommentCreatedEvent(
                comment.getId(),
                comment.getArticleId(),
                articleTitle,
                comment.getUserId(),
                userNickname,
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }

    private String getArticleTitle(UUID articleId) {
        try {
            return articleQueryApi.getTitle(articleId);
        } catch (ArticleNotFoundException exception) {
            throw new CommentArticleNotFoundException(articleId);
        }
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<CommentDto> search(
            UUID articleId, UUID requesterId, String orderBy, SortDirection direction, CursorRequest request) {
        if (!articleQueryApi.exists(articleId)) {
            throw new CommentArticleNotFoundException(articleId);
        }
        CommentOrderBy commentOrderBy = CommentOrderBy.from(orderBy);

        List<CommentSearchResult> comments =
                commentRepository.findByArticleId(articleId, requesterId, commentOrderBy, direction, request);
        long totalCommentCount = commentRepository.countByArticleId(articleId);

        CursorPageResponse<CommentSearchResult> page = CursorPageResponse.of(
                comments,
                request.limit(),
                totalCommentCount,
                result -> result.comment().getId().toString(),
                result -> result.comment().getCreatedAt());

        Set<UUID> authorIds = page.content().stream()
                .map(result -> result.comment().getUserId())
                .collect(Collectors.toSet());

        Map<UUID, UserSummary> usersById = userQueryApi.getSummaries(authorIds);

        List<CommentDto> dtoContent = page.content().stream()
                .map(result -> {
                    Comment comment = result.comment();

                    UserSummary user = usersById.get(comment.getUserId());
                    if (user == null) {
                        throw new CommentUserNotFoundException(comment.getUserId());
                    }

                    return CommentDto.from(comment, user.nickname(), result.likedByMe());
                })
                .toList();

        return new CursorPageResponse<>(
                dtoContent, page.nextCursor(), page.nextAfter(), page.size(), page.totalElements(), page.hasNext());
    }
}

package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.api.CommentUpdatedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLikeRepository;
import com.sprint.monew.comment.domain.CommentOrderBy;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.domain.CommentSearchResult;
import com.sprint.monew.comment.domain.exception.CommentArticleNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.domain.exception.InvalidCommentCursorException;
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
    private final CommentLikeRepository commentLikeRepository;
    private final ArticleQueryApi articleQueryApi;
    private final ArticleCommandApi articleCommandApi;
    private final UserQueryApi userQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    // 댓글을 등록하고 기사 댓글 수 증가 및 생성 이벤트 발행을 처리한다.
    @Transactional
    public CommentDto register(UUID articleId, UUID userId, String content) {

        // getSummary()는 탈퇴 사용자도 반환할 수 있으므로, 댓글 등록 전 활동 가능한 사용자 여부를 확인한다.
        if (!userQueryApi.exists(userId)) {
            throw new CommentUserNotFoundException(userId);
        }

        UserSummary user = userQueryApi.getSummary(userId);

        if (!articleQueryApi.exists(articleId)) {
            throw new CommentArticleNotFoundException(articleId);
        }

        String articleTitle = articleQueryApi.getTitle(articleId);

        Comment comment = commentRepository.save(new Comment(articleId, userId, content));
        articleCommandApi.increaseCommentCount(articleId);
        eventPublisher.publishEvent(toCreatedEvent(comment, articleTitle, user.nickname()));

        return CommentDto.from(comment, user.nickname(), false);
    }

    // 기사별 댓글을 정렬·커서 페이지 방식으로 조회한다.
    @Transactional(readOnly = true)
    public CursorPageResponse<CommentDto> search(
            UUID articleId, UUID requesterId, String orderBy, SortDirection direction, CursorRequest request) {
        if (!articleQueryApi.exists(articleId)) {
            throw new CommentArticleNotFoundException(articleId);
        }
        CommentOrderBy commentOrderBy = CommentOrderBy.from(orderBy);
        validateCursorRequest(request, commentOrderBy);

        List<CommentSearchResult> comments =
                commentRepository.findByArticleId(articleId, requesterId, commentOrderBy, direction, request);
        long totalCommentCount = commentRepository.countByArticleId(articleId);

        CursorPageResponse<CommentSearchResult> page = CursorPageResponse.of(
                comments,
                request.limit(),
                totalCommentCount,
                result -> cursorOf(result, commentOrderBy),
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

    @Transactional
    public CommentDto update(UUID commentId, UUID userId, String content) {
        Comment comment = commentRepository
                .findById(commentId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        comment.updateContent(userId, content);
        UserSummary user = userQueryApi.getSummary(userId);
        boolean likeByMe = commentLikeRepository.existsByComment_IdAndUserId(commentId, userId);

        eventPublisher.publishEvent(toUpdatedEvent(comment));

        return CommentDto.from(comment, user.nickname(), likeByMe);
    }

    // ========================================================================================
    // 내부 헬퍼 메서드

    // 저장된 댓글을 다른 모듈에 전달할 생성 이벤트로 변환한다.
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

    private CommentUpdatedEvent toUpdatedEvent(Comment comment) {
        return new CommentUpdatedEvent(comment.getId(), comment.getUserId(), comment.getContent());
    }

    // 정렬 기준에 맞는 다음 페이지용 cursor 문자열을 생성한다.
    private String cursorOf(CommentSearchResult result, CommentOrderBy orderBy) {
        Comment comment = result.comment();

        return switch (orderBy) {
            case CREATED_AT -> comment.getId().toString();
            case LIKE_COUNT -> comment.getLikeCount() + "_" + comment.getId();
        };
    }

    // cursor와 after의 조합 및 정렬 기준별 cursor 형식을 검증한다.
    private void validateCursorRequest(CursorRequest request, CommentOrderBy orderBy) {
        if (request.cursor() == null && request.after() == null) {
            return;
        }

        if (request.cursor() == null || request.after() == null) {
            throw new InvalidCommentCursorException(request.cursor());
        }

        try {
            switch (orderBy) {
                case CREATED_AT -> UUID.fromString(request.cursor());
                case LIKE_COUNT -> parseLikeCountCursor(request.cursor());
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidCommentCursorException(request.cursor());
        }
    }

    // 좋아요 수 정렬 cursor의 "likeCount_commentId" 값을 검증한다.
    private void parseLikeCountCursor(String cursor) {
        String[] values = cursor.split("_", 2);

        if (values.length != 2) {
            throw new IllegalArgumentException();
        }

        Long.parseLong(values[0]);
        UUID.fromString(values[1]);
    }
}

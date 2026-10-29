package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentLikedEvent;
import com.sprint.monew.comment.api.CommentUnlikedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.exception.CommentLikeAlreadyExistsException;
import com.sprint.monew.comment.domain.exception.CommentLikeNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.comment.web.dto.CommentLikeDto;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ArticleQueryApi articleQueryApi;
    private final UserQueryApi userQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentLikeDto like(UUID commentId, UUID requesterUserId) {

        Comment comment =
                commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!userQueryApi.exists(requesterUserId)) {
            throw new CommentUserNotFoundException(requesterUserId);
        }

        // 이미 commentLike에 요청자가 그 댓글에 좋아요를 좋아요를 눌렀다면 true -> 에러로감.
        if (commentLikeRepository.existsByComment_IdAndUserId(commentId, requesterUserId)) {
            throw new CommentLikeAlreadyExistsException(commentId, requesterUserId);
        }

        UserSummary requester = userQueryApi.getSummary(requesterUserId);
        UserSummary commentUser = userQueryApi.getSummary(comment.getUserId());

        CommentLike savedLike =
                commentLikeRepository.save(new CommentLike(comment, requesterUserId, comment.getArticleId()));

        comment.increaseLikeCount();

        eventPublisher.publishEvent(
                toLikedEvent(savedLike, requester, commentUser, articleQueryApi.getTitle(comment.getArticleId())));

        return CommentLikeDto.from(savedLike, commentUser.nickname());
    }

    @Transactional
    public void unlike(UUID commentId, UUID requesterUserId) {

        Comment comment =
                commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!userQueryApi.exists(requesterUserId)) {
            throw new CommentUserNotFoundException(requesterUserId);
        }

        int deletedCount = commentLikeRepository.deleteByCommentIdAndUserId(commentId, requesterUserId);

        if (deletedCount == 0) {
            throw new CommentLikeNotFoundException(commentId, requesterUserId);
        }

        comment.decreaseLikeCount();

        eventPublisher.publishEvent(
                toUnlikedEvent(commentId, requesterUserId, comment.getUserId(), comment.getLikeCount()));
    }

    private CommentLikedEvent toLikedEvent(
            CommentLike commentLike, UserSummary liker, UserSummary commentUser, String articleTitle) {
        Comment comment = commentLike.getComment();

        return new CommentLikedEvent(
                commentLike.getId(),
                commentLike.getCreatedAt(),
                liker.id(),
                liker.nickname(),
                comment.getId(),
                comment.getArticleId(),
                articleTitle,
                comment.getUserId(),
                commentUser.nickname(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }

    private CommentUnlikedEvent toUnlikedEvent(
            UUID commentId, UUID likerId, UUID commentUserId, long commentLikeCount) {
        return new CommentUnlikedEvent(commentId, likerId, commentUserId, commentLikeCount);
    }
}

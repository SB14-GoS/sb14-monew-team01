package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CommentUserDeletedHandler {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ArticleCommandApi articleCommandApi;

    @EventListener
    @Transactional
    /* 유저 물리삭제 후 연관 관계 정리*/
    public void onUserDeleted(UserDeletedEvent event) {
        UUID userId = event.userId();

        List<UUID> likedCommentIds = commentLikeRepository.findAllByUserId(userId).stream()
                .map(commentLike -> commentLike.getComment().getId())
                .toList();

        likedCommentIds.forEach(commentRepository::decreaseLikeCount);

        commentLikeRepository.deleteAllByUserId(userId);

        commentRepository.findAllByUserId(userId).stream()
                .filter(comment -> !comment.isDeleted())
                .map(Comment::getArticleId)
                .forEach(articleCommandApi::decreaseCommentCount);
        commentRepository.deleteAllByUserId(userId);
    }
}

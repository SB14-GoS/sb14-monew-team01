package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLikeRepository;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
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
    public void onUserDeleted(UserDeletedEvent event) {
        UUID userId = event.userId();

        commentLikeRepository.findAllByUserId(userId).stream()
                .map(commentLike -> commentLike.getComment())
                .forEach(Comment::decreaseLikeCount);
        commentLikeRepository.deleteAllByUserId(userId);

        commentRepository.findAllByUserId(userId).stream()
                .filter(comment -> !comment.isDeleted())
                .map(Comment::getArticleId)
                .forEach(articleCommandApi::decreaseCommentCount);
        commentRepository.deleteAllByUserId(userId);
    }
}

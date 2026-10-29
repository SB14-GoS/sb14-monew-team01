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

        // 삭제할 사용자가 좋아요를 누른 댓글의 ID를 모은다.
        List<UUID> likedCommentIds = commentLikeRepository.findAllByUserId(userId).stream()
                .map(commentLike -> commentLike.getComment().getId())
                .toList();

        /*
           해당 댓글들의 좋아요 수를 감소시키고 사용자의 좋아요를 삭제.
           좋아요에 취소에 대한 중복 처리 방지
        */
        for (UUID commentId : likedCommentIds) {
            int deletedCount = commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

            if (deletedCount > 0) {
                commentRepository.decreaseLikeCount(commentId);
            }
        }

        // 삭제할 사용자가 작성한 댓글을 조회한다.
        List<Comment> comments = commentRepository.findAllByUserId(userId);

        List<UUID> commentIds = comments.stream().map(Comment::getId).toList();

        // 다른 사용자가 누른 좋아요를 포함해, 삭제할 댓글의 좋아요를 모두 삭제한다.
        if (!commentIds.isEmpty()) {
            commentLikeRepository.deleteAllByCommentIdIn(commentIds);
        }

        // 활성 댓글만 기사 댓글 수에서 차감한다.
        comments.stream()
                .filter(comment -> !comment.isDeleted())
                .map(Comment::getArticleId)
                .forEach(articleCommandApi::decreaseCommentCount);

        // 좋아요 정리 후 댓글을 물리 삭제한다.
        commentRepository.deleteAllByUserId(userId);
    }
}

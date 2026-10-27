package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentCleanupServiceIntegrationTest {

    @Autowired
    private CommentCleanupService commentCleanupService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 기사에_속한_활성_및_논리삭제_댓글과_좋아요를_물리삭제하고_다른_기사_데이터는_보존한다() {
        UUID targetArticleId = UUID.randomUUID();
        UUID otherArticleId = UUID.randomUUID();
        Comment activeComment = saveComment(targetArticleId);
        Comment softDeletedComment = saveComment(targetArticleId);
        Comment otherArticleComment = saveComment(otherArticleId);
        softDeletedComment.softDelete();

        CommentLike activeCommentLike = saveLike(activeComment);
        CommentLike softDeletedCommentLike = saveLike(softDeletedComment);
        CommentLike otherArticleCommentLike = saveLike(otherArticleComment);
        entityManager.flush();
        entityManager.clear();

        commentCleanupService.deleteAllByArticleId(targetArticleId);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(activeComment.getId())).isEmpty();
        assertThat(commentRepository.findById(softDeletedComment.getId())).isEmpty();
        assertThat(commentLikeRepository.findById(activeCommentLike.getId())).isEmpty();
        assertThat(commentLikeRepository.findById(softDeletedCommentLike.getId()))
                .isEmpty();

        assertThat(commentRepository.findById(otherArticleComment.getId())).isPresent();
        assertThat(commentLikeRepository.findById(otherArticleCommentLike.getId()))
                .isPresent();
    }

    private Comment saveComment(UUID articleId) {
        return commentRepository.saveAndFlush(new Comment(articleId, UUID.randomUUID(), "댓글 내용"));
    }

    private CommentLike saveLike(Comment comment) {
        return commentLikeRepository.saveAndFlush(new CommentLike(comment, UUID.randomUUID(), comment.getArticleId()));
    }
}

package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.api.UserDeletedEvent;
import com.sprint.monew.user.api.UserQueryApi;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class CommentUserDeletedConcurrencyIntegrationTest {
    @Autowired
    CommentRepository comments;

    @Autowired
    CommentLikeRepository likes;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 사용자_삭제가_좋아요를_조회한_후_취소되어도_카운트를_한번만_감소한다() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID deletedUser = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID commentId = transaction.execute(status -> {
            Comment comment = comments.saveAndFlush(new Comment(articleId, UUID.randomUUID(), "댓글"));
            likes.saveAndFlush(new CommentLike(comment, deletedUser, articleId));
            likes.saveAndFlush(new CommentLike(comment, otherUser, articleId));
            comments.increaseLikeCount(comment.getId());
            comments.increaseLikeCount(comment.getId());
            return comment.getId();
        });

        CountDownLatch likesRead = new CountDownLatch(1);
        CountDownLatch unlikeFinished = new CountDownLatch(1);
        CommentLikeRepository coordinatedLikes =
                mock(CommentLikeRepository.class, AdditionalAnswers.delegatesTo(likes));
        doAnswer(invocation -> {
                    var found = likes.findAllByUserId(deletedUser);
                    likesRead.countDown();
                    if (!unlikeFinished.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("좋아요 취소를 기다리는 시간이 초과되었습니다.");
                    }
                    return found;
                })
                .when(coordinatedLikes)
                .findAllByUserId(deletedUser);
        CommentUserDeletedHandler handler =
                new CommentUserDeletedHandler(comments, coordinatedLikes, mock(ArticleCommandApi.class));
        UserQueryApi users = mock(UserQueryApi.class);
        when(users.exists(deletedUser)).thenReturn(true);
        CommentLikeService service = new CommentLikeService(
                comments, likes, mock(ArticleQueryApi.class), users, mock(ApplicationEventPublisher.class));
        var executor = Executors.newSingleThreadExecutor();
        try {
            var deletion = executor.submit(() -> transaction.executeWithoutResult(
                    status -> handler.onUserDeleted(new UserDeletedEvent(deletedUser))));
            assertThat(likesRead.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                transaction.executeWithoutResult(status -> service.unlike(commentId, deletedUser));
            } finally {
                unlikeFinished.countDown();
            }
            deletion.get(20, TimeUnit.SECONDS);

            assertThat(comments.findById(commentId).orElseThrow().getLikeCount())
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject(
                            "select count(*) from comment_likes where comment_id = ?", Long.class, commentId))
                    .isEqualTo(1L);
            assertThat(likes.existsByComment_IdAndUserId(commentId, otherUser)).isTrue();
            assertThat(likes.existsByComment_IdAndUserId(commentId, deletedUser))
                    .isFalse();
        } finally {
            unlikeFinished.countDown();
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> {
                likes.deleteAllByArticleId(articleId);
                comments.deleteAllByArticleId(articleId);
            });
        }
    }
}

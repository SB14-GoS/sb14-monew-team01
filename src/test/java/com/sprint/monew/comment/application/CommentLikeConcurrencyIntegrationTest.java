package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.exception.CommentLikeAlreadyExistsException;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class CommentLikeConcurrencyIntegrationTest {
    @Autowired
    CommentRepository comments;

    @Autowired
    CommentLikeRepository likes;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void 동시_중복_요청은_한번만_저장하고_나머지는_409로_변환한다() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = comments.saveAndFlush(new Comment(articleId, authorId, "댓글"));
        UUID commentId = comment.getId();
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        CommentLikeRepository coordinatedLikes =
                mock(CommentLikeRepository.class, AdditionalAnswers.delegatesTo(likes));
        when(coordinatedLikes.existsByComment_IdAndUserId(commentId, userId)).thenAnswer(invocation -> {
            boolean exists = likes.existsByComment_IdAndUserId(commentId, userId);
            bothChecked.await(10, TimeUnit.SECONDS);
            return exists;
        });
        UserQueryApi users = mock(UserQueryApi.class);
        when(users.exists(userId)).thenReturn(true);
        when(users.getSummary(any(UUID.class)))
                .thenAnswer(invocation -> new UserSummary(invocation.getArgument(0), "사용자"));
        ArticleQueryApi articles = mock(ArticleQueryApi.class);
        when(articles.getTitle(articleId)).thenReturn("기사");
        CommentLikeService service = new CommentLikeService(
                comments, coordinatedLikes, articles, users, mock(ApplicationEventPublisher.class));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> executeLike(transaction, service, commentId, userId));
            var second = executor.submit(() -> executeLike(transaction, service, commentId, userId));
            var outcomes = java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(outcomes).filteredOn(result -> result.equals("success")).hasSize(1);
            assertThat(outcomes)
                    .filteredOn(CommentLikeAlreadyExistsException.class::isInstance)
                    .hasSize(1);
            CommentLikeAlreadyExistsException failure = (CommentLikeAlreadyExistsException) outcomes.stream()
                    .filter(CommentLikeAlreadyExistsException.class::isInstance)
                    .findFirst()
                    .orElseThrow();
            assertThat(failure.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(failure.getCause()).isNotNull();
            assertThat(likes.findAllByUserId(userId)).hasSize(1);
            assertThat(comments.findById(commentId).orElseThrow().getLikeCount())
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> {
                likes.deleteAllByArticleId(articleId);
                comments.deleteAllByArticleId(articleId);
            });
        }
    }

    private Object executeLike(
            TransactionTemplate transaction, CommentLikeService service, UUID commentId, UUID userId) {
        try {
            transaction.executeWithoutResult(status -> service.like(commentId, userId));
            return "success";
        } catch (RuntimeException exception) {
            return exception;
        }
    }
}

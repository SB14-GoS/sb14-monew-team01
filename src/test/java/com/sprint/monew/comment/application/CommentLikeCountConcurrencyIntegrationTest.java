package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class CommentLikeCountConcurrencyIntegrationTest {
    @Autowired
    CommentRepository comments;

    @Autowired
    CommentLikeRepository likes;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 서로_다른_사용자의_동시_좋아요는_두번_증가한다() throws Exception {
        runConcurrentChanges(false, false, 2);
    }

    @Test
    void 서로_다른_사용자의_동시_좋아요_취소는_두번_감소한다() throws Exception {
        runConcurrentChanges(true, true, 0);
    }

    @Test
    void 좋아요와_취소를_동시에_수행하면_실제_좋아요_수와_일치한다() throws Exception {
        runConcurrentChanges(true, false, 1);
    }

    private void runConcurrentChanges(boolean firstUnlike, boolean secondUnlike, long expectedCount) throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID commentId = transaction.execute(status -> {
            Comment comment = comments.saveAndFlush(new Comment(articleId, authorId, "동시성 테스트 댓글"));
            if (firstUnlike) {
                likes.saveAndFlush(new CommentLike(comment, firstUser, articleId));
                comments.increaseLikeCount(comment.getId());
            }
            if (secondUnlike) {
                Comment current = comments.findById(comment.getId()).orElseThrow();
                likes.saveAndFlush(new CommentLike(current, secondUser, articleId));
                comments.increaseLikeCount(comment.getId());
            }
            return comment.getId();
        });

        CyclicBarrier bothRead = new CyclicBarrier(2);
        ThreadLocal<Boolean> alreadyRead = ThreadLocal.withInitial(() -> false);
        CommentRepository coordinatedComments = mock(CommentRepository.class, AdditionalAnswers.delegatesTo(comments));
        when(coordinatedComments.findById(commentId)).thenAnswer(invocation -> {
            var result = comments.findById(commentId);
            if (!alreadyRead.get()) {
                alreadyRead.set(true);
                bothRead.await(10, TimeUnit.SECONDS);
            }
            return result;
        });
        UserQueryApi users = mock(UserQueryApi.class);
        when(users.exists(any(UUID.class))).thenReturn(true);
        when(users.getSummary(any(UUID.class)))
                .thenAnswer(invocation -> new UserSummary(invocation.getArgument(0), "사용자"));
        ArticleQueryApi articles = mock(ArticleQueryApi.class);
        when(articles.getTitle(articleId)).thenReturn("기사");
        CommentLikeService service = new CommentLikeService(
                coordinatedComments, likes, articles, users, mock(ApplicationEventPublisher.class));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> execute(transaction, service, commentId, firstUser, firstUnlike));
            var second = executor.submit(() -> execute(transaction, service, commentId, secondUser, secondUnlike));
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);

            long storedCount = comments.findById(commentId).orElseThrow().getLikeCount();
            Long actualLikes = jdbc.queryForObject(
                    "select count(*) from comment_likes where comment_id = ?", Long.class, commentId);
            assertThat(storedCount).isEqualTo(expectedCount);
            assertThat(actualLikes).isEqualTo(expectedCount);
        } finally {
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> {
                likes.deleteAllByArticleId(articleId);
                comments.deleteAllByArticleId(articleId);
            });
        }
    }

    private void execute(
            TransactionTemplate transaction, CommentLikeService service, UUID commentId, UUID userId, boolean unlike) {
        transaction.executeWithoutResult(status -> {
            if (unlike) {
                service.unlike(commentId, userId);
            } else {
                service.like(commentId, userId);
            }
        });
    }
}

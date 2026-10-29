package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.application.UserService;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentUserDeletedIntegrationTest {
    @Autowired
    UserService userService;

    @Autowired
    UserRepository users;

    @Autowired
    CommentRepository comments;

    @Autowired
    CommentLikeRepository likes;

    @Autowired
    EntityManager entityManager;

    @Test
    void 다른_사용자가_좋아요한_댓글의_작성자를_삭제하면_연관_좋아요를_먼저_삭제한다() {
        User author = users.save(new User(UUID.randomUUID() + "@example.com", "작성자", "password"));
        User other = users.save(new User(UUID.randomUUID() + "@example.com", "다른사용자", "password"));
        Article article = Article.create(
                "기사", Instant.now(), "요약", ArticleSource.NAVER, "https://example.com/" + UUID.randomUUID());
        entityManager.persist(article);
        Comment active = comments.save(new Comment(article.getId(), author.getId(), "활성 댓글"));
        Comment deleted = comments.save(new Comment(article.getId(), author.getId(), "삭제된 댓글"));
        deleted.softDelete();
        Comment surviving = comments.save(new Comment(article.getId(), other.getId(), "다른 사용자의 댓글"));
        CommentLike activeLike = likes.save(new CommentLike(active, other.getId(), article.getId()));
        CommentLike deletedLike = likes.save(new CommentLike(deleted, other.getId(), article.getId()));
        CommentLike authorLike = likes.save(new CommentLike(surviving, author.getId(), article.getId()));
        CommentLike survivingLike = likes.save(new CommentLike(surviving, other.getId(), article.getId()));
        entityManager.flush();
        comments.increaseLikeCount(surviving.getId());
        comments.increaseLikeCount(surviving.getId());

        userService.hardDelete(author.getId(), author.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(users.findById(author.getId())).isEmpty();
        assertThat(users.findById(other.getId())).isPresent();
        assertThat(comments.findById(active.getId())).isEmpty();
        assertThat(comments.findById(deleted.getId())).isEmpty();
        assertThat(likes.findById(activeLike.getId())).isEmpty();
        assertThat(likes.findById(deletedLike.getId())).isEmpty();
        assertThat(likes.findById(authorLike.getId())).isEmpty();
        assertThat(likes.findById(survivingLike.getId())).isPresent();
        assertThat(comments.findById(surviving.getId()).orElseThrow().getLikeCount())
                .isEqualTo(1);
    }
}

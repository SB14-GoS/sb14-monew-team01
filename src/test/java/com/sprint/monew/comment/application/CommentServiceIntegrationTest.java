package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 좋아요가_있는_활성_댓글을_물리삭제하면_외래키_위반_없이_댓글과_좋아요가_삭제된다() {
        UUID authorId = UUID.randomUUID();
        Article article = Article.create(
                "기사 제목", Instant.now(), "기사 요약", ArticleSource.NAVER, "https://news.example/" + UUID.randomUUID());
        entityManager.persist(article);
        entityManager.flush();

        UUID articleId = article.getId();
        Comment comment = commentRepository.saveAndFlush(new Comment(articleId, authorId, "댓글 내용"));
        CommentLike like = commentLikeRepository.saveAndFlush(new CommentLike(comment, UUID.randomUUID(), articleId));
        entityManager.clear();

        commentService.deleteHard(comment.getId(), authorId);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
        assertThat(commentLikeRepository.findById(like.getId())).isEmpty();
    }
}

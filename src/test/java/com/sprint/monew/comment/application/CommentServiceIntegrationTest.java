package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 댓글_목록_HTTP_요청이_실제_서비스와_DB를_통해_200을_반환한다() throws Exception {
        User author = new User(UUID.randomUUID() + "@example.com", "댓글작성자", "encoded-password");
        entityManager.persist(author);
        Article article = Article.create(
                "기사 제목", Instant.now(), "기사 요약", ArticleSource.NAVER, "https://news.example/" + UUID.randomUUID());
        entityManager.persist(article);
        entityManager.flush();
        Comment comment = commentRepository.saveAndFlush(new Comment(article.getId(), author.getId(), "댓글 내용"));
        entityManager.clear();

        mockMvc.perform(get("/api/comments")
                        .header("Monew-Request-User-ID", author.getId())
                        .param("articleId", article.getId().toString())
                        .param("orderBy", "createdAt")
                        .param("direction", "DESC")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(comment.getId().toString()))
                .andExpect(jsonPath("$.content[0].userNickname").value("댓글작성자"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 댓글_목록을_커서로_조회하면_삭제된_댓글을_제외하고_모든_페이지를_반환한다() {
        User author = new User(UUID.randomUUID() + "@example.com", "댓글작성자", "encoded-password");
        entityManager.persist(author);
        Article article = Article.create(
                "기사 제목", Instant.now(), "기사 요약", ArticleSource.NAVER, "https://news.example/" + UUID.randomUUID());
        entityManager.persist(article);
        entityManager.flush();

        UUID articleId = article.getId();
        UUID authorId = author.getId();
        Comment first = commentRepository.saveAndFlush(new Comment(articleId, authorId, "첫 댓글"));
        Comment second = commentRepository.saveAndFlush(new Comment(articleId, authorId, "두 번째 댓글"));
        Comment deleted = commentRepository.saveAndFlush(new Comment(articleId, authorId, "삭제된 댓글"));
        deleted.softDelete();
        entityManager.flush();
        entityManager.clear();

        var firstPage = commentService.search(
                articleId, authorId, "createdAt", SortDirection.ASC, new CursorRequest(null, null, 1));
        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.content().get(0).userNickname()).isEqualTo("댓글작성자");
        assertThat(firstPage.totalElements()).isEqualTo(2L);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotNull();
        assertThat(firstPage.nextAfter()).isNotNull();

        var secondPage = commentService.search(
                articleId,
                authorId,
                "createdAt",
                SortDirection.ASC,
                new CursorRequest(firstPage.nextCursor(), firstPage.nextAfter(), 1));
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.totalElements()).isEqualTo(2L);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(List.of(
                        firstPage.content().get(0).id(),
                        secondPage.content().get(0).id()))
                .containsExactlyInAnyOrder(first.getId(), second.getId())
                .doesNotHaveDuplicates();
    }

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

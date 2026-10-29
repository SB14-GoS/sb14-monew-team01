package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentLikedEvent;
import com.sprint.monew.comment.api.CommentUnlikedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.exception.CommentLikeAlreadyExistsException;
import com.sprint.monew.comment.domain.exception.CommentLikeNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.comment.web.dto.CommentLikeDto;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private ArticleQueryApi articleQueryApi;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentLikeService commentLikeService;

    @Test
    void 댓글에_좋아요를_등록하면_좋아요_수를_증가시키고_이벤트를_발행한다() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        UUID commentUserId = UUID.randomUUID();
        Comment comment = new Comment(articleId, commentUserId, "댓글 내용");
        ReflectionTestUtils.setField(comment, "id", commentId);
        Comment updatedComment = new Comment(articleId, commentUserId, "댓글 내용");
        ReflectionTestUtils.setField(updatedComment, "id", commentId);
        ReflectionTestUtils.setField(updatedComment, "likeCount", 1L);
        UserSummary requester = new UserSummary(requesterUserId, "좋아요한 사용자");
        UserSummary commentUser = new UserSummary(commentUserId, "댓글 작성자");
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment), Optional.of(updatedComment));
        given(userQueryApi.exists(requesterUserId)).willReturn(true);
        given(commentLikeRepository.existsByComment_IdAndUserId(commentId, requesterUserId))
                .willReturn(false);
        given(userQueryApi.getSummary(requesterUserId)).willReturn(requester);
        given(userQueryApi.getSummary(commentUserId)).willReturn(commentUser);
        given(articleQueryApi.getTitle(articleId)).willReturn("기사 제목");
        given(commentLikeRepository.saveAndFlush(any(CommentLike.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CommentLikeDto response = commentLikeService.like(commentId, requesterUserId);

        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeRepository).saveAndFlush(likeCaptor.capture());
        assertThat(likeCaptor.getValue())
                .extracting(CommentLike::getComment, CommentLike::getUserId, CommentLike::getArticleId)
                .containsExactly(updatedComment, requesterUserId, articleId);
        verify(commentRepository).increaseLikeCount(commentId);
        assertThat(response)
                .extracting(
                        CommentLikeDto::likedBy,
                        CommentLikeDto::commentId,
                        CommentLikeDto::articleId,
                        CommentLikeDto::commentUserId,
                        CommentLikeDto::commentUserNickname,
                        CommentLikeDto::commentContent,
                        CommentLikeDto::commentLikeCount)
                .containsExactly(requesterUserId, commentId, articleId, commentUserId, "댓글 작성자", "댓글 내용", 1L);

        ArgumentCaptor<CommentLikedEvent> eventCaptor = ArgumentCaptor.forClass(CommentLikedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(
                        CommentLikedEvent::likerId,
                        CommentLikedEvent::likerNickname,
                        CommentLikedEvent::articleId,
                        CommentLikedEvent::articleTitle,
                        CommentLikedEvent::commentUserId,
                        CommentLikedEvent::commentUserNickname,
                        CommentLikedEvent::commentContent,
                        CommentLikedEvent::commentLikeCount)
                .containsExactly(requesterUserId, "좋아요한 사용자", articleId, "기사 제목", commentUserId, "댓글 작성자", "댓글 내용", 1L);
    }

    @Test
    void 존재하지_않는_댓글에는_좋아요를_등록할_수_없다() {
        UUID commentId = UUID.randomUUID();
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentLikeService.like(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentNotFoundException.class);

        then(userQueryApi).shouldHaveNoInteractions();
        then(commentLikeRepository).shouldHaveNoInteractions();
        then(articleQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 논리_삭제된_댓글에는_좋아요를_등록할_수_없다() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment(UUID.randomUUID(), UUID.randomUUID(), "댓글 내용");
        comment.softDelete();
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentLikeService.like(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentNotFoundException.class);

        then(commentRepository).should(org.mockito.Mockito.never()).increaseLikeCount(commentId);
        then(commentLikeRepository).shouldHaveNoInteractions();
        then(userQueryApi).shouldHaveNoInteractions();
        then(articleQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 논리_삭제된_댓글의_좋아요는_취소할_수_없다() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment(UUID.randomUUID(), UUID.randomUUID(), "댓글 내용");
        comment.softDelete();
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentLikeService.unlike(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentNotFoundException.class);

        then(commentRepository).should(org.mockito.Mockito.never()).decreaseLikeCount(commentId);
        then(commentLikeRepository).shouldHaveNoInteractions();
        then(userQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_사용자는_좋아요를_등록할_수_없다() {
        UUID commentId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(new Comment(UUID.randomUUID(), UUID.randomUUID(), "댓글 내용")));
        given(userQueryApi.exists(requesterUserId)).willReturn(false);

        assertThatThrownBy(() -> commentLikeService.like(commentId, requesterUserId))
                .isInstanceOf(CommentUserNotFoundException.class);

        then(commentLikeRepository).shouldHaveNoInteractions();
        then(articleQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 이미_좋아요한_댓글에는_다시_좋아요를_등록할_수_없다() {
        UUID commentId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(new Comment(UUID.randomUUID(), UUID.randomUUID(), "댓글 내용")));
        given(userQueryApi.exists(requesterUserId)).willReturn(true);
        given(commentLikeRepository.existsByComment_IdAndUserId(commentId, requesterUserId))
                .willReturn(true);

        assertThatThrownBy(() -> commentLikeService.like(commentId, requesterUserId))
                .isInstanceOf(CommentLikeAlreadyExistsException.class);

        then(articleQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 동시_중복_저장시_좋아요_제약_위반을_원인을_보존한_중복_예외로_변환한다() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        prepareLikeForSave(commentId, userId);
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "duplicate",
                new ConstraintViolationException(
                        "duplicate", new SQLException("duplicate", "23505"), "uk_comment_like_comment_user"));
        given(commentLikeRepository.saveAndFlush(any(CommentLike.class))).willThrow(failure);

        assertThatThrownBy(() -> commentLikeService.like(commentId, userId))
                .isInstanceOf(CommentLikeAlreadyExistsException.class)
                .hasCause(failure);
        verify(commentRepository).increaseLikeCount(commentId);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 좋아요_제약이_아닌_무결성_위반은_그대로_전파한다() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        prepareLikeForSave(commentId, userId);
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "other",
                new ConstraintViolationException("other", new SQLException("duplicate", "23505"), "other_constraint"));
        given(commentLikeRepository.saveAndFlush(any(CommentLike.class))).willThrow(failure);

        assertThatThrownBy(() -> commentLikeService.like(commentId, userId)).isSameAs(failure);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    private Comment prepareLikeForSave(UUID commentId, UUID userId) {
        UUID authorId = UUID.randomUUID();
        Comment comment = new Comment(UUID.randomUUID(), authorId, "댓글 내용");
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(userQueryApi.exists(userId)).willReturn(true);
        given(commentLikeRepository.existsByComment_IdAndUserId(commentId, userId))
                .willReturn(false);
        given(userQueryApi.getSummary(userId)).willReturn(new UserSummary(userId, "사용자"));
        given(userQueryApi.getSummary(authorId)).willReturn(new UserSummary(authorId, "작성자"));
        return comment;
    }

    @Test
    void 댓글_좋아요를_취소하면_좋아요_수를_감소시키고_이벤트를_발행한다() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        UUID commentUserId = UUID.randomUUID();
        Comment comment = new Comment(articleId, commentUserId, "댓글 내용");
        ReflectionTestUtils.setField(comment, "id", commentId);
        ReflectionTestUtils.setField(comment, "likeCount", 1L);
        Comment updatedComment = new Comment(articleId, commentUserId, "댓글 내용");
        ReflectionTestUtils.setField(updatedComment, "id", commentId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment), Optional.of(updatedComment));
        given(userQueryApi.exists(requesterUserId)).willReturn(true);
        given(commentLikeRepository.deleteByCommentIdAndUserId(commentId, requesterUserId))
                .willReturn(1);

        commentLikeService.unlike(commentId, requesterUserId);

        then(commentLikeRepository).should().deleteByCommentIdAndUserId(commentId, requesterUserId);
        verify(commentRepository).decreaseLikeCount(commentId);
        ArgumentCaptor<CommentUnlikedEvent> eventCaptor = ArgumentCaptor.forClass(CommentUnlikedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(
                        CommentUnlikedEvent::commentId,
                        CommentUnlikedEvent::likerId,
                        CommentUnlikedEvent::commentUserId,
                        CommentUnlikedEvent::commentLikeCount)
                .containsExactly(commentId, requesterUserId, commentUserId, 0L);
    }

    @Test
    void 존재하지_않는_댓글의_좋아요는_취소할_수_없다() {
        UUID commentId = UUID.randomUUID();
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentLikeService.unlike(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentNotFoundException.class);

        then(userQueryApi).shouldHaveNoInteractions();
        then(commentLikeRepository).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_사용자는_좋아요를_취소할_수_없다() {
        UUID commentId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(new Comment(UUID.randomUUID(), UUID.randomUUID(), "댓글 내용")));
        given(userQueryApi.exists(requesterUserId)).willReturn(false);

        assertThatThrownBy(() -> commentLikeService.unlike(commentId, requesterUserId))
                .isInstanceOf(CommentUserNotFoundException.class);

        then(commentLikeRepository).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 좋아요하지_않은_댓글의_좋아요는_취소할_수_없다() {
        UUID commentId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        Comment comment = new Comment(UUID.randomUUID(), UUID.randomUUID(), "댓글 내용");
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(userQueryApi.exists(requesterUserId)).willReturn(true);
        given(commentLikeRepository.deleteByCommentIdAndUserId(commentId, requesterUserId))
                .willReturn(0);

        assertThatThrownBy(() -> commentLikeService.unlike(commentId, requesterUserId))
                .isInstanceOf(CommentLikeNotFoundException.class);

        assertThat(comment.getLikeCount()).isZero();
        then(eventPublisher).shouldHaveNoInteractions();
    }
}

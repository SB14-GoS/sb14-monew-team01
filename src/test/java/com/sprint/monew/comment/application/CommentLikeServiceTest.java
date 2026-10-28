package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentLikedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.CommentLikeRepository;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.domain.exception.CommentLikeAlreadyExistsException;
import com.sprint.monew.comment.domain.exception.CommentNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.web.dto.CommentLikeDto;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
        UserSummary requester = new UserSummary(requesterUserId, "좋아요한 사용자");
        UserSummary commentUser = new UserSummary(commentUserId, "댓글 작성자");
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(userQueryApi.exists(requesterUserId)).willReturn(true);
        given(commentLikeRepository.existsByComment_IdAndUserId(commentId, requesterUserId))
                .willReturn(false);
        given(userQueryApi.getSummary(requesterUserId)).willReturn(requester);
        given(userQueryApi.getSummary(commentUserId)).willReturn(commentUser);
        given(articleQueryApi.getTitle(articleId)).willReturn("기사 제목");
        given(commentLikeRepository.save(any(CommentLike.class))).willAnswer(invocation -> invocation.getArgument(0));

        CommentLikeDto response = commentLikeService.like(commentId, requesterUserId);

        ArgumentCaptor<CommentLike> likeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeRepository).save(likeCaptor.capture());
        assertThat(likeCaptor.getValue())
                .extracting(CommentLike::getComment, CommentLike::getUserId, CommentLike::getArticleId)
                .containsExactly(comment, requesterUserId, articleId);
        assertThat(comment.getLikeCount()).isEqualTo(1L);
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
}

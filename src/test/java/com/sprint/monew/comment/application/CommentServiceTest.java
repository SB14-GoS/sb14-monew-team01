package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.api.CommentDeletedEvent;
import com.sprint.monew.comment.api.CommentUpdatedEvent;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentOrderBy;
import com.sprint.monew.comment.domain.exception.CommentAccessDeniedException;
import com.sprint.monew.comment.domain.exception.CommentArticleNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.comment.domain.exception.InvalidCommentCursorException;
import com.sprint.monew.comment.domain.exception.InvalidCommentOrderByException;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
import com.sprint.monew.comment.domain.repository.CommentSearchResult;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private ArticleQueryApi articleQueryApi;

    @Mock
    private ArticleCommandApi articleCommandApi;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    @Test
    void 존재하는_기사에_댓글을_등록하면_기사_댓글_수를_증가시킨다() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(userQueryApi.exists(userId)).willReturn(true);
        given(userQueryApi.getSummary(userId)).willReturn(new UserSummary(userId, "댓글작성자"));
        given(articleQueryApi.exists(articleId)).willReturn(true);
        given(articleQueryApi.getTitle(articleId)).willReturn("기사 제목");
        given(commentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        var comment = commentService.register(articleId, userId, "좋은 기사네요.");

        then(commentRepository).should().save(any());
        then(articleQueryApi).should().exists(articleId);
        then(articleQueryApi).should().getTitle(articleId);
        then(articleCommandApi).should().increaseCommentCount(articleId);
        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(
                        CommentCreatedEvent::articleId,
                        CommentCreatedEvent::articleTitle,
                        CommentCreatedEvent::userId,
                        CommentCreatedEvent::userNickname,
                        CommentCreatedEvent::content,
                        CommentCreatedEvent::likeCount)
                .containsExactly(articleId, "기사 제목", userId, "댓글작성자", "좋은 기사네요.", 0L);
        assertThat(comment.userNickname()).isEqualTo("댓글작성자");
        assertThat(comment.likedByMe()).isFalse();
    }

    @Test
    void 존재하지_않는_기사에는_댓글을_등록할_수_없다() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(userQueryApi.exists(userId)).willReturn(true);
        given(articleQueryApi.exists(articleId)).willReturn(false);

        assertThatThrownBy(() -> commentService.register(articleId, userId, "댓글"))
                .isInstanceOf(CommentArticleNotFoundException.class);

        then(commentRepository).shouldHaveNoInteractions();
        then(articleCommandApi).shouldHaveNoInteractions();
        then(articleQueryApi).should(never()).getTitle(articleId);
    }

    @Test
    void 존재하지_않는_사용자는_댓글을_등록할_수_없다() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(userQueryApi.exists(userId)).willReturn(false);

        assertThatThrownBy(() -> commentService.register(articleId, userId, "댓글"))
                .isInstanceOf(CommentUserNotFoundException.class);

        then(commentRepository).shouldHaveNoInteractions();
        then(articleCommandApi).shouldHaveNoInteractions();
        then(articleQueryApi).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_기사의_댓글은_조회할_수_없다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(false);

        assertThatThrownBy(() -> commentService.search(
                        articleId,
                        UUID.randomUUID(),
                        "createdAt",
                        SortDirection.ASC,
                        new CursorRequest(null, null, 10)))
                .isInstanceOf(CommentArticleNotFoundException.class);

        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    void 댓글_목록을_조회하면_정렬_조건과_요청자_ID를_전달하고_커서_응답을_만든다() {
        UUID articleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        Instant firstCreatedAt = Instant.parse("2026-10-26T01:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-10-26T01:01:00Z");
        CursorRequest request = new CursorRequest(null, null, 2);

        Comment firstComment = mockComment(articleId, firstUserId, firstCreatedAt, 10L);
        Comment secondComment = mockComment(articleId, secondUserId, secondCreatedAt, 5L);
        Comment thirdComment = mock(Comment.class);

        given(articleQueryApi.exists(articleId)).willReturn(true);
        given(commentRepository.findByArticleId(
                        articleId, requesterId, CommentOrderBy.LIKE_COUNT, SortDirection.DESC, request))
                .willReturn(List.of(
                        new CommentSearchResult(firstComment, true),
                        new CommentSearchResult(secondComment, false),
                        new CommentSearchResult(thirdComment, true)));
        given(commentRepository.countByArticleId(articleId)).willReturn(3L);
        given(userQueryApi.getSummaries(Set.of(firstUserId, secondUserId)))
                .willReturn(Map.of(
                        firstUserId, new UserSummary(firstUserId, "작성자1"),
                        secondUserId, new UserSummary(secondUserId, "작성자2")));

        var response = commentService.search(articleId, requesterId, "likeCount", SortDirection.DESC, request);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).extracting(CommentDto::likedByMe).containsExactly(true, false);
        assertThat(response)
                .extracting(
                        responseValue -> responseValue.hasNext(),
                        responseValue -> responseValue.size(),
                        responseValue -> responseValue.totalElements(),
                        responseValue -> responseValue.nextCursor(),
                        responseValue -> responseValue.nextAfter())
                .containsExactly(true, 2, 3L, "5_" + secondComment.getId(), secondCreatedAt);
        then(commentRepository)
                .should()
                .findByArticleId(articleId, requesterId, CommentOrderBy.LIKE_COUNT, SortDirection.DESC, request);
        then(userQueryApi).should().getSummaries(Set.of(firstUserId, secondUserId));
    }

    @Test
    void cursor와_after_중_하나만_전달하면_조회_전에_예외가_발생한다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(true);

        assertThatThrownBy(() -> commentService.search(
                        articleId,
                        UUID.randomUUID(),
                        "createdAt",
                        SortDirection.ASC,
                        new CursorRequest(UUID.randomUUID().toString(), null, 10)))
                .isInstanceOf(InvalidCommentCursorException.class);

        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    void 생성순_cursor가_UUID_형식이_아니면_조회_전에_예외가_발생한다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(true);

        assertThatThrownBy(() -> commentService.search(
                        articleId,
                        UUID.randomUUID(),
                        "createdAt",
                        SortDirection.ASC,
                        new CursorRequest("not-a-uuid", Instant.now(), 10)))
                .isInstanceOf(InvalidCommentCursorException.class);

        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    void 좋아요순_cursor가_형식에_맞지_않으면_조회_전에_예외가_발생한다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(true);

        assertThatThrownBy(() -> commentService.search(
                        articleId,
                        UUID.randomUUID(),
                        "likeCount",
                        SortDirection.ASC,
                        new CursorRequest("not-a-like-count-cursor", Instant.now(), 10)))
                .isInstanceOf(InvalidCommentCursorException.class);

        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    void 지원하지_않는_정렬_속성으로_댓글을_조회하면_예외가_발생한다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(true);

        assertThatThrownBy(() -> commentService.search(
                        articleId,
                        UUID.randomUUID(),
                        "unsupported",
                        SortDirection.ASC,
                        new CursorRequest(null, null, 10)))
                .isInstanceOf(InvalidCommentOrderByException.class);

        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    void 작성자가_댓글을_수정하면_수정된_댓글을_반환한다() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-26T01:00:00Z");
        Comment comment = mockComment(articleId, userId, createdAt, 3L);
        given(comment.getId()).willReturn(commentId);
        given(comment.getContent()).willReturn("수정한 댓글");
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(userQueryApi.getSummary(userId)).willReturn(new UserSummary(userId, "작성자"));
        given(commentLikeRepository.existsByComment_IdAndUserId(commentId, userId))
                .willReturn(true);

        CommentDto response = commentService.update(commentId, userId, "수정한 댓글");

        verify(comment).updateContent(userId, "수정한 댓글");
        ArgumentCaptor<CommentUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(CommentUpdatedEvent::commentId, CommentUpdatedEvent::userId, CommentUpdatedEvent::content)
                .containsExactly(commentId, userId, "수정한 댓글");
        assertThat(response)
                .extracting(CommentDto::userNickname, CommentDto::likeCount, CommentDto::likedByMe)
                .containsExactly("작성자", 3L, true);
    }

    @Test
    void 존재하지_않는_댓글은_수정할_수_없다() {
        UUID commentId = UUID.randomUUID();
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(commentId, UUID.randomUUID(), "수정한 댓글"))
                .isInstanceOf(CommentNotFoundException.class);

        then(userQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 작성자가_아니면_댓글을_수정할_수_없다() {
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        org.mockito.Mockito.doThrow(new CommentAccessDeniedException(commentId, requesterId))
                .when(comment)
                .updateContent(requesterId, "수정한 댓글");

        assertThatThrownBy(() -> commentService.update(commentId, requesterId, "수정한 댓글"))
                .isInstanceOf(CommentAccessDeniedException.class);

        then(userQueryApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 작성자가_댓글을_삭제하면_논리삭제하고_기사_댓글_수를_감소시킨다() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getId()).willReturn(commentId);
        given(comment.getArticleId()).willReturn(articleId);
        given(comment.getUserId()).willReturn(userId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.softDeleteIfActive(commentId)).willReturn(1);

        commentService.delete(commentId, userId);

        then(commentRepository).should().softDeleteIfActive(commentId);
        then(articleCommandApi).should().decreaseCommentCount(articleId);
        ArgumentCaptor<CommentDeletedEvent> eventCaptor = ArgumentCaptor.forClass(CommentDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(CommentDeletedEvent::commentId, CommentDeletedEvent::userId)
                .containsExactly(commentId, userId);
    }

    @Test
    void 다른_요청이_먼저_삭제한_댓글은_카운트를_감소시키거나_삭제_이벤트를_발행하지_않는다() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getUserId()).willReturn(userId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.softDeleteIfActive(commentId)).willReturn(0);

        commentService.delete(commentId, userId);

        then(commentRepository).should().softDeleteIfActive(commentId);
        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void 존재하지_않거나_이미_삭제된_댓글은_삭제할_수_없다(boolean deleted) {
        UUID commentId = UUID.randomUUID();
        if (deleted) {
            Comment comment = mock(Comment.class);
            given(comment.isDeleted()).willReturn(true);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        } else {
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());
        }

        assertThatThrownBy(() -> commentService.delete(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentNotFoundException.class);

        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 작성자가_아닌_사용자는_댓글을_삭제할_수_없다() {
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getUserId()).willReturn(authorId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentAccessDeniedException.class);

        verify(comment, never()).softDelete();
        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 활성_댓글을_물리삭제하면_기사_댓글_수를_감소시키고_삭제_이벤트를_발행한다() {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getId()).willReturn(commentId);
        given(comment.getArticleId()).willReturn(articleId);
        given(comment.getUserId()).willReturn(userId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.hardDeleteIfActive(commentId)).willReturn(1);

        commentService.deleteHard(commentId, userId);

        then(commentLikeRepository).should().deleteAllByCommentId(commentId);
        then(commentRepository).should().hardDeleteIfActive(commentId);
        then(commentRepository).should(never()).delete(comment);
        then(articleCommandApi).should().decreaseCommentCount(articleId);
        ArgumentCaptor<CommentDeletedEvent> eventCaptor = ArgumentCaptor.forClass(CommentDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(CommentDeletedEvent::commentId, CommentDeletedEvent::userId)
                .containsExactly(commentId, userId);
    }

    @Test
    void 논리삭제된_댓글을_물리삭제하면_기사_댓글_수는_감소시키지_않는다() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getUserId()).willReturn(userId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.hardDeleteIfActive(commentId)).willReturn(0);

        commentService.deleteHard(commentId, userId);

        then(commentLikeRepository).should().deleteAllByCommentId(commentId);
        then(commentRepository).should().hardDeleteIfActive(commentId);
        then(commentRepository).should().delete(comment);
        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 다른_요청이_먼저_논리삭제한_활성_댓글을_물리삭제하면_후속_처리를_수행하지_않는다() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getUserId()).willReturn(userId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.hardDeleteIfActive(commentId)).willReturn(0);

        commentService.deleteHard(commentId, userId);

        then(commentLikeRepository).should().deleteAllByCommentId(commentId);
        then(commentRepository).should().hardDeleteIfActive(commentId);
        then(commentRepository).should().delete(comment);
        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 작성자가_아닌_사용자는_댓글을_물리삭제할_수_없다() {
        UUID commentId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        given(comment.getUserId()).willReturn(UUID.randomUUID());
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteHard(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentAccessDeniedException.class);

        then(commentRepository).should(never()).delete(comment);
        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_댓글은_물리삭제할_수_없다() {
        UUID commentId = UUID.randomUUID();
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteHard(commentId, UUID.randomUUID()))
                .isInstanceOf(CommentNotFoundException.class);

        then(articleCommandApi).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    private Comment mockComment(UUID articleId, UUID userId, Instant createdAt, long likeCount) {
        Comment comment = mock(Comment.class);
        given(comment.getId()).willReturn(UUID.randomUUID());
        given(comment.getArticleId()).willReturn(articleId);
        given(comment.getUserId()).willReturn(userId);
        given(comment.getContent()).willReturn("댓글 내용");
        given(comment.getLikeCount()).willReturn(likeCount);
        given(comment.getCreatedAt()).willReturn(createdAt);
        return comment;
    }
}

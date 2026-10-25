package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.comment.domain.exception.CommentArticleNotFoundException;
import com.sprint.monew.comment.domain.exception.CommentUserNotFoundException;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        given(articleQueryApi.exists(articleId)).willReturn(true);
        given(userQueryApi.exists(userId)).willReturn(true);
        given(userQueryApi.getSummary(userId)).willReturn(new UserSummary(userId, "댓글작성자"));
        given(articleQueryApi.getTitle(articleId)).willReturn("기사 제목");
        given(commentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        var comment = commentService.register(articleId, userId, "좋은 기사네요.");

        then(commentRepository).should().save(any());
        then(articleCommandApi).should().increaseCommentCount(articleId);
        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue())
                .extracting(
                        CommentCreatedEvent::articleId,
                        CommentCreatedEvent::articleTitle,
                        CommentCreatedEvent::userId,
                        CommentCreatedEvent::userNickname,
                        CommentCreatedEvent::content,
                        CommentCreatedEvent::likeCount)
                .containsExactly(articleId, "기사 제목", userId, "댓글작성자", "좋은 기사네요.", 0L);
        org.assertj.core.api.Assertions.assertThat(comment.userNickname()).isEqualTo("댓글작성자");
        org.assertj.core.api.Assertions.assertThat(comment.likedByMe()).isFalse();
    }

    @Test
    void 존재하지_않는_기사에는_댓글을_등록할_수_없다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(false);

        assertThatThrownBy(() -> commentService.register(articleId, UUID.randomUUID(), "댓글"))
                .isInstanceOf(CommentArticleNotFoundException.class);

        then(commentRepository).shouldHaveNoInteractions();
        then(articleCommandApi).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_사용자는_댓글을_등록할_수_없다() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(true);
        given(userQueryApi.exists(userId)).willReturn(false);

        assertThatThrownBy(() -> commentService.register(articleId, userId, "댓글"))
                .isInstanceOf(CommentUserNotFoundException.class);

        then(commentRepository).shouldHaveNoInteractions();
        then(articleCommandApi).shouldHaveNoInteractions();
    }

    @Test
    void 존재하지_않는_기사의_댓글은_조회할_수_없다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(false);

        assertThatThrownBy(() -> commentService.search(articleId, new CursorRequest(null, null, 10)))
                .isInstanceOf(CommentArticleNotFoundException.class);

        then(commentRepository).shouldHaveNoInteractions();
    }
}

package com.sprint.monew.comment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.domain.CommentErrorCode;
import com.sprint.monew.comment.domain.CommentException;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        given(commentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        var comment = commentService.register(articleId, userId, "좋은 기사네요.");

        then(commentRepository).should().save(any());
        then(articleCommandApi).should().increaseCommentCount(articleId);
        then(eventPublisher).should().publishEvent(any(CommentCreatedEvent.class));
        org.assertj.core.api.Assertions.assertThat(comment.userNickname()).isEqualTo("댓글작성자");
        org.assertj.core.api.Assertions.assertThat(comment.likedByMe()).isFalse();
    }

    @Test
    void 존재하지_않는_기사에는_댓글을_등록할_수_없다() {
        UUID articleId = UUID.randomUUID();
        given(articleQueryApi.exists(articleId)).willReturn(false);

        assertThatThrownBy(() -> commentService.register(articleId, UUID.randomUUID(), "댓글"))
                .isInstanceOf(CommentException.class)
                .extracting("errorCode")
                .isEqualTo(CommentErrorCode.ARTICLE_NOT_FOUND);

        then(commentRepository).shouldHaveNoInteractions();
        then(articleCommandApi).shouldHaveNoInteractions();
    }
}

package com.sprint.monew.comment.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.CommentLikeRepository;
import com.sprint.monew.comment.domain.CommentRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentUserDeletedHandlerTest {

    @Mock
    CommentRepository commentRepository;

    @Mock
    CommentLikeRepository commentLikeRepository;

    @Mock
    ArticleCommandApi articleCommandApi;

    @InjectMocks
    CommentUserDeletedHandler handler;

    @Test
    @DisplayName("사용자 삭제 시 좋아요 수와 댓글 수를 낮추고 좋아요를 댓글보다 먼저 삭제한다")
    void onUserDeleted() {
        UUID deletedUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        CommentLike commentLike = mock(CommentLike.class);
        Comment likedComment = mock(Comment.class);
        Comment writtenComment = mock(Comment.class);

        given(commentLike.getComment()).willReturn(likedComment);
        given(writtenComment.isDeleted()).willReturn(false);
        given(writtenComment.getArticleId()).willReturn(articleId);
        given(commentLikeRepository.findAllByUserId(deletedUserId)).willReturn(List.of(commentLike));
        given(commentRepository.findAllByUserId(deletedUserId)).willReturn(List.of(writtenComment));

        handler.onUserDeleted(new UserDeletedEvent(deletedUserId));

        verify(likedComment).decreaseLikeCount();
        verify(articleCommandApi).decreaseCommentCount(articleId);

        InOrder deletionOrder = inOrder(commentLikeRepository, commentRepository);
        deletionOrder.verify(commentLikeRepository).deleteAllByUserId(deletedUserId);
        deletionOrder.verify(commentRepository).deleteAllByUserId(deletedUserId);
    }
}

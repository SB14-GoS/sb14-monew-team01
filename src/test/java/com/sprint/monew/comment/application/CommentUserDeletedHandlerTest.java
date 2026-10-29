package com.sprint.monew.comment.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.repository.CommentLikeRepository;
import com.sprint.monew.comment.domain.repository.CommentRepository;
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
        UUID likedCommentId = UUID.randomUUID();
        UUID writtenCommentId = UUID.randomUUID();
        CommentLike commentLike = mock(CommentLike.class);
        Comment likedComment = mock(Comment.class);
        Comment writtenComment = mock(Comment.class);

        given(commentLike.getComment()).willReturn(likedComment);
        given(likedComment.getId()).willReturn(likedCommentId);
        given(writtenComment.getId()).willReturn(writtenCommentId);
        given(writtenComment.isDeleted()).willReturn(false);
        given(writtenComment.getArticleId()).willReturn(articleId);
        given(commentLikeRepository.findAllByUserId(deletedUserId)).willReturn(List.of(commentLike));
        given(commentLikeRepository.deleteByCommentIdAndUserId(likedCommentId, deletedUserId))
                .willReturn(1);
        given(commentRepository.findAllByUserId(deletedUserId)).willReturn(List.of(writtenComment));

        handler.onUserDeleted(new UserDeletedEvent(deletedUserId));

        verify(commentRepository).decreaseLikeCount(likedCommentId);
        verify(articleCommandApi).decreaseCommentCount(articleId);

        InOrder deletionOrder = inOrder(commentLikeRepository, commentRepository);
        deletionOrder.verify(commentLikeRepository).deleteByCommentIdAndUserId(likedCommentId, deletedUserId);
        deletionOrder.verify(commentRepository).decreaseLikeCount(likedCommentId);
        deletionOrder.verify(commentLikeRepository).deleteAllByCommentIdIn(List.of(writtenCommentId));
        deletionOrder.verify(commentRepository).deleteAllByUserId(deletedUserId);
    }

    @Test
    @DisplayName("사용자 삭제 시 이미 소프트 삭제된 댓글은 기사 댓글 수를 낮추지 않는다")
    void onUserDeleted_skipsSoftDeletedCommentsWhenDecreasingArticleCommentCount() {
        UUID deletedUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment softDeletedComment = mock(Comment.class);
        given(softDeletedComment.getId()).willReturn(commentId);
        given(softDeletedComment.isDeleted()).willReturn(true);
        given(commentLikeRepository.findAllByUserId(deletedUserId)).willReturn(List.of());
        given(commentRepository.findAllByUserId(deletedUserId)).willReturn(List.of(softDeletedComment));

        handler.onUserDeleted(new UserDeletedEvent(deletedUserId));

        verifyNoInteractions(articleCommandApi);
        verify(commentLikeRepository).deleteAllByCommentIdIn(List.of(commentId));
        verify(commentRepository).deleteAllByUserId(deletedUserId);
    }

    @Test
    @DisplayName("작성한 댓글이 없으면 댓글 ID 목록 삭제 쿼리를 실행하지 않는다")
    void onUserDeleted_withoutWrittenComments() {
        UUID userId = UUID.randomUUID();
        given(commentLikeRepository.findAllByUserId(userId)).willReturn(List.of());
        given(commentRepository.findAllByUserId(userId)).willReturn(List.of());

        handler.onUserDeleted(new UserDeletedEvent(userId));

        verify(commentLikeRepository, never()).deleteAllByCommentIdIn(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("이미 취소된 좋아요는 카운트를 다시 감소시키지 않는다")
    void onUserDeleted_doesNotDecreaseIfLikeAlreadyDeleted() {
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        CommentLike like = mock(CommentLike.class);
        Comment comment = mock(Comment.class);
        given(like.getComment()).willReturn(comment);
        given(comment.getId()).willReturn(commentId);
        given(commentLikeRepository.findAllByUserId(userId)).willReturn(List.of(like));
        given(commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId))
                .willReturn(0);
        given(commentRepository.findAllByUserId(userId)).willReturn(List.of());

        handler.onUserDeleted(new UserDeletedEvent(userId));

        verify(commentRepository, never()).decreaseLikeCount(commentId);
    }
}

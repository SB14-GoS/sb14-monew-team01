package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.api.CommentDeletedEvent;
import com.sprint.monew.comment.api.CommentLikedEvent;
import com.sprint.monew.comment.api.CommentUnlikedEvent;
import com.sprint.monew.comment.api.CommentUpdatedEvent;
import com.sprint.monew.interest.api.InterestDeletedEvent;
import com.sprint.monew.interest.api.InterestSubscribedEvent;
import com.sprint.monew.interest.api.InterestUnsubscribedEvent;
import com.sprint.monew.user.api.UserDeletedEvent;
import com.sprint.monew.user.api.UserNicknameUpdatedEvent;
import com.sprint.monew.user.api.UserRegisteredEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityProjectorTest {

    @Mock
    UserActivityWriter writer;

    @InjectMocks
    UserActivityProjector projector;

    // 필드마다 다른 값을 써서 인자가 뒤바뀌면 테스트가 실패하게 한다
    private final UUID userId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final UUID commentId = UUID.randomUUID();
    private final UUID commentUserId = UUID.randomUUID();
    private final Instant firstTime = Instant.parse("2026-10-26T01:00:00Z");
    private final Instant secondTime = Instant.parse("2026-10-26T02:00:00Z");
    private final Instant thirdTime = Instant.parse("2026-10-26T03:00:00Z");

    @Test
    @DisplayName("기사 조회 이벤트는 본 사람 문서에 viewedBy·createdAt까지 옮겨 추가한다")
    void onArticleViewed() {
        UUID articleViewId = UUID.randomUUID();
        ArticleViewedEvent event = new ArticleViewedEvent(
                articleViewId,
                userId,
                firstTime,
                articleId,
                "NAVER",
                "https://news.example.com/1",
                "기사 제목",
                secondTime,
                "요약",
                3L,
                7L);

        projector.onArticleViewed(event);

        ArgumentCaptor<ArticleViewItem> captor = ArgumentCaptor.forClass(ArticleViewItem.class);
        verify(writer).addArticleView(eq(userId), captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new ArticleViewItem(
                        articleViewId,
                        userId,
                        firstTime,
                        articleId,
                        "NAVER",
                        "https://news.example.com/1",
                        "기사 제목",
                        secondTime,
                        "요약",
                        3L,
                        7L));
    }

    @Test
    @DisplayName("구독 이벤트는 subscribedAt을 createdAt으로 옮겨 구독을 추가한다")
    void onInterestSubscribed() {
        UUID subscriptionId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        InterestSubscribedEvent event = new InterestSubscribedEvent(
                subscriptionId, userId, interestId, "경제", List.of("주식", "금리"), 5L, firstTime);

        projector.onInterestSubscribed(event);

        verify(writer)
                .addSubscription(
                        userId,
                        new SubscriptionItem(subscriptionId, interestId, "경제", List.of("주식", "금리"), 5L, firstTime));
    }

    @Test
    @DisplayName("구독 취소·관심사 삭제 이벤트는 해당 Writer 메서드를 순서대로 호출한다")
    void onInterestUnsubscribedAndDeleted() {
        UUID interestId = UUID.randomUUID();

        projector.onInterestUnsubscribed(new InterestUnsubscribedEvent(userId, interestId));
        projector.onInterestDeleted(new InterestDeletedEvent(interestId));

        verify(writer).removeSubscription(userId, interestId);
        verify(writer).removeInterestFromAll(interestId);
    }

    @Test
    @DisplayName("사용자 가입·닉네임 변경·삭제 이벤트는 문서 생성·수정·삭제로 이어진다")
    void onUserEvents() {
        projector.onUserRegistered(new UserRegisteredEvent(userId, "user@monew.com", "닉네임", firstTime));
        projector.onUserNicknameUpdated(new UserNicknameUpdatedEvent(userId, "새닉네임"));
        projector.onUserDeleted(new UserDeletedEvent(userId));

        verify(writer).create(userId, "user@monew.com", "닉네임", firstTime);
        verify(writer).updateNickname(userId, "새닉네임");
        verify(writer).delete(userId);
    }

    @Test
    @DisplayName("댓글 작성 이벤트는 작성자 문서에 댓글 항목을 추가한다")
    void onCommentCreated() {
        CommentCreatedEvent event =
                new CommentCreatedEvent(commentId, articleId, "기사 제목", userId, "작성자", "댓글 내용", 0L, firstTime);

        projector.onCommentCreated(event);

        verify(writer)
                .addComment(
                        userId, new CommentItem(commentId, articleId, "기사 제목", userId, "작성자", "댓글 내용", 0L, firstTime));
    }

    @Test
    @DisplayName("댓글 수정·삭제 이벤트는 (userId, commentId) 순서로 Writer를 호출한다")
    void onCommentUpdatedAndDeleted() {
        projector.onCommentUpdated(new CommentUpdatedEvent(commentId, userId, "수정된 내용"));
        projector.onCommentDeleted(new CommentDeletedEvent(commentId, userId));

        verify(writer).updateCommentContent(userId, commentId, "수정된 내용");
        verify(writer).removeComment(userId, commentId);
    }

    @Test
    @DisplayName("좋아요 이벤트는 누른 사람 문서에 좋아요를 추가하고 작성자 문서의 좋아요 수를 바꾼다")
    void onCommentLiked() {
        UUID likerId = UUID.randomUUID();
        UUID commentLikeId = UUID.randomUUID();

        projector.onCommentLiked(commentLikedEvent(commentLikeId, likerId));

        ArgumentCaptor<CommentLikeItem> captor = ArgumentCaptor.forClass(CommentLikeItem.class);
        verify(writer).addCommentLike(eq(likerId), captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new CommentLikeItem(
                        commentLikeId,
                        firstTime,
                        commentId,
                        articleId,
                        "기사 제목",
                        commentUserId,
                        "작성자",
                        "댓글 내용",
                        4L,
                        thirdTime));
        verify(writer).updateCommentLikeCount(commentUserId, commentId, 4L);
    }

    @Test
    @DisplayName("좋아요 취소 이벤트는 누른 사람 문서에서 빼고 작성자 문서의 좋아요 수를 바꾼다")
    void onCommentUnliked() {
        UUID likerId = UUID.randomUUID();

        projector.onCommentUnliked(new CommentUnlikedEvent(commentId, likerId, commentUserId, 2L));

        verify(writer).removeCommentLike(likerId, commentId);
        verify(writer).updateCommentLikeCount(commentUserId, commentId, 2L);
    }

    @Test
    @DisplayName("Writer가 실패해도 리스너는 예외를 밖으로 던지지 않는다")
    void swallowsWriterException() {
        willThrow(new RuntimeException("mongo down")).given(writer).addArticleView(any(), any());
        ArticleViewedEvent event = new ArticleViewedEvent(
                UUID.randomUUID(), userId, firstTime, articleId, "NAVER", "url", "제목", secondTime, "요약", 0L, 1L);

        assertThatCode(() -> projector.onArticleViewed(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("좋아요 추가가 실패해도 작성자 좋아요 수 갱신은 실행된다")
    void likeCountUpdatedEvenIfAddFails() {
        UUID likerId = UUID.randomUUID();
        willThrow(new RuntimeException("mongo down")).given(writer).addCommentLike(any(), any());

        assertThatCode(() -> projector.onCommentLiked(commentLikedEvent(UUID.randomUUID(), likerId)))
                .doesNotThrowAnyException();
        verify(writer).updateCommentLikeCount(eq(commentUserId), eq(commentId), anyLong());
    }

    private CommentLikedEvent commentLikedEvent(UUID commentLikeId, UUID likerId) {
        return new CommentLikedEvent(
                commentLikeId,
                firstTime,
                likerId,
                "좋아요누른사람",
                commentId,
                articleId,
                "기사 제목",
                commentUserId,
                "작성자",
                "댓글 내용",
                4L,
                thirdTime);
    }
}

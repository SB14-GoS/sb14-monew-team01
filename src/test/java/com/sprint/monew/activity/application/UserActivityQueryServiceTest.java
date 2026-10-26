package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.activity.domain.ActivityErrorCode;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.exception.UserActivityNotFoundException;
import com.sprint.monew.activity.web.dto.UserActivityDto;
import com.sprint.monew.user.api.UserQueryApi;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityQueryServiceTest {

    @Mock
    UserActivityRepository userActivityRepository;

    @Mock
    UserQueryApi userQueryApi;

    @InjectMocks
    UserActivityQueryService userActivityQueryService;

    private final UUID userId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-10-26T00:00:00Z");

    @Test
    @DisplayName("활동 가능한 사용자의 문서가 있으면 항목 ID를 id로 바꿔 DTO로 반환한다")
    void getActivity() {
        UUID subscriptionId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID commentLikeId = UUID.randomUUID();
        UUID articleViewId = UUID.randomUUID();
        UserActivityDocument document = new UserActivityDocument(userId, "user@monew.com", "닉네임", createdAt);
        document.getSubscriptions()
                .add(new SubscriptionItem(subscriptionId, UUID.randomUUID(), "경제", List.of("주식"), 3L, createdAt));
        document.getComments()
                .add(new CommentItem(commentId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 1L, createdAt));
        document.getCommentLikes()
                .add(new CommentLikeItem(
                        commentLikeId,
                        createdAt,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기사",
                        UUID.randomUUID(),
                        "작성자",
                        "내용",
                        2L,
                        createdAt));
        document.getArticleViews()
                .add(new ArticleViewItem(
                        articleViewId,
                        userId,
                        createdAt,
                        UUID.randomUUID(),
                        "NAVER",
                        "https://news.example.com/1",
                        "기사",
                        createdAt,
                        "요약",
                        0L,
                        1L));
        given(userQueryApi.exists(userId)).willReturn(true);
        given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

        UserActivityDto result = userActivityQueryService.getActivity(userId);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo("user@monew.com");
        assertThat(result.nickname()).isEqualTo("닉네임");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.subscriptions().get(0).id()).isEqualTo(subscriptionId);
        assertThat(result.comments().get(0).id()).isEqualTo(commentId);
        assertThat(result.commentLikes().get(0).id()).isEqualTo(commentLikeId);
        assertThat(result.articleViews().get(0).id()).isEqualTo(articleViewId);
    }

    @Test
    @DisplayName("없거나 탈퇴한 사용자면 USER_ACTIVITY_NOT_FOUND를 던지고 문서를 조회하지 않는다")
    void inactiveUser() {
        given(userQueryApi.exists(userId)).willReturn(false);

        assertThatThrownBy(() -> userActivityQueryService.getActivity(userId))
                .isInstanceOf(UserActivityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ActivityErrorCode.USER_ACTIVITY_NOT_FOUND);
        verify(userActivityRepository, never()).findById(any());
    }

    @Test
    @DisplayName("사용자는 있지만 활동 문서가 없으면 USER_ACTIVITY_NOT_FOUND를 던진다")
    void missingDocument() {
        given(userQueryApi.exists(userId)).willReturn(true);
        given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userActivityQueryService.getActivity(userId))
                .isInstanceOf(UserActivityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ActivityErrorCode.USER_ACTIVITY_NOT_FOUND);
    }
}

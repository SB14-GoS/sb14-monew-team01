package com.sprint.monew.activity.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.activity.application.UserActivityQueryService;
import com.sprint.monew.activity.domain.exception.UserActivityNotFoundException;
import com.sprint.monew.activity.web.dto.ArticleViewActivityDto;
import com.sprint.monew.activity.web.dto.CommentActivityDto;
import com.sprint.monew.activity.web.dto.CommentLikeActivityDto;
import com.sprint.monew.activity.web.dto.SubscriptionActivityDto;
import com.sprint.monew.activity.web.dto.UserActivityDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserActivityController.class)
class UserActivityControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserActivityQueryService userActivityQueryService;

    private final UUID userId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-10-26T00:00:00Z");

    @Test
    @DisplayName("헤더 없이 조회해도 200과 명세 필드 이름으로 응답한다")
    void getActivity() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID commentLikeId = UUID.randomUUID();
        UUID articleViewId = UUID.randomUUID();
        UserActivityDto dto = new UserActivityDto(
                userId,
                "user@monew.com",
                "닉네임",
                createdAt,
                List.of(new SubscriptionActivityDto(
                        subscriptionId, UUID.randomUUID(), "경제", List.of("주식"), 3L, createdAt)),
                List.of(new CommentActivityDto(commentId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 1L, createdAt)),
                List.of(new CommentLikeActivityDto(
                        commentLikeId,
                        createdAt,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기사",
                        UUID.randomUUID(),
                        "작성자",
                        "내용",
                        2L,
                        createdAt)),
                List.of(new ArticleViewActivityDto(
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
                        1L)));
        given(userActivityQueryService.getActivity(userId)).willReturn(dto);

        mockMvc.perform(get("/api/user-activities/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@monew.com"))
                .andExpect(jsonPath("$.nickname").value("닉네임"))
                .andExpect(jsonPath("$.subscriptions[0].id").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.subscriptions[0].interestKeywords[0]").value("주식"))
                .andExpect(jsonPath("$.comments[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.comments[0].likeCount").value(1))
                .andExpect(jsonPath("$.commentLikes[0].id").value(commentLikeId.toString()))
                .andExpect(jsonPath("$.commentLikes[0].commentLikeCount").value(2))
                .andExpect(jsonPath("$.articleViews[0].id").value(articleViewId.toString()))
                .andExpect(jsonPath("$.articleViews[0].viewedBy").value(userId.toString()))
                .andExpect(jsonPath("$.articleViews[0].articleViewCount").value(1));
    }

    @Test
    @DisplayName("활동 내역이 없으면 404와 USER_ACTIVITY_NOT_FOUND를 반환한다")
    void notFound() throws Exception {
        given(userActivityQueryService.getActivity(userId)).willThrow(new UserActivityNotFoundException(userId));

        mockMvc.perform(get("/api/user-activities/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_ACTIVITY_NOT_FOUND"));
    }

    @Test
    @DisplayName("userId가 UUID 형식이 아니면 서비스 호출 전에 400을 반환한다")
    void invalidUserId() throws Exception {
        mockMvc.perform(get("/api/user-activities/{userId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(userActivityQueryService);
    }
}

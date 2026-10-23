package com.sprint.monew.notification.web;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.notification.application.NotificationService;
import com.sprint.monew.notification.domain.exception.NotificationNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("알림 확인은 200과 빈 본문을 반환한다")
    void confirm() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(patch("/api/notifications/{id}", notificationId).header("Monew-Request-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(notificationService).confirm(notificationId, userId);
    }

    @Test
    @DisplayName("없거나 남의 알림이면 404를 반환한다")
    void confirmNotFound() throws Exception {
        UUID notificationId = UUID.randomUUID();
        willThrow(new NotificationNotFoundException(notificationId))
                .given(notificationService)
                .confirm(notificationId, userId);

        mockMvc.perform(patch("/api/notifications/{id}", notificationId).header("Monew-Request-User-ID", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("알림 ID가 UUID 형식이 아니면 서비스 호출 전에 400을 반환한다")
    void confirmInvalidId() throws Exception {
        mockMvc.perform(patch("/api/notifications/{id}", "not-a-uuid").header("Monew-Request-User-ID", userId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("전체 확인은 200과 빈 본문을 반환한다")
    void confirmAll() throws Exception {
        mockMvc.perform(patch("/api/notifications").header("Monew-Request-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(notificationService).confirmAll(userId);
    }

    @Test
    @DisplayName("사용자 ID 헤더가 없으면 서비스 호출 전에 400을 반환한다")
    void rejectsMissingUserId() throws Exception {
        mockMvc.perform(patch("/api/notifications")).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/notifications/{id}", UUID.randomUUID())).andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }
}

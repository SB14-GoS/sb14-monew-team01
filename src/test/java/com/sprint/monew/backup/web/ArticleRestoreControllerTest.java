package com.sprint.monew.backup.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.backup.application.ArticleRestoreService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleRestoreController.class)
class ArticleRestoreControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ArticleRestoreService articleRestoreService;

    @Test
    @DisplayName("기간을 받아 날짜별 복구 결과를 명세 필드 이름으로 응답한다")
    void restore() throws Exception {
        UUID restoredId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 10, 28, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 28, 23, 59, 59);
        given(articleRestoreService.restore(from, to))
                .willReturn(List.of(
                        ArticleRestoreResultDto.of(Instant.parse("2026-10-27T15:00:00Z"), List.of(restoredId))));

        mockMvc.perform(get("/api/articles/restore")
                        .param("from", "2026-10-28T00:00:00")
                        .param("to", "2026-10-28T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restoreDate").value("2026-10-27T15:00:00Z"))
                .andExpect(jsonPath("$[0].restoredArticleIds[0]").value(restoredId.toString()))
                .andExpect(jsonPath("$[0].restoredArticleCount").value(1));
    }
}

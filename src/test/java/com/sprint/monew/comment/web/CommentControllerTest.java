package com.sprint.monew.comment.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.comment.application.CommentService;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommentService commentService;

    private final UUID userId = UUID.randomUUID();

    @ParameterizedTest
    @ValueSource(strings = {"{\"content\":\"댓글\"}", "{\"articleId\":null,\"content\":\"댓글\"}"})
    void articleId가_누락되거나_null이면_서비스_호출_전에_필드_검증_오류를_반환한다(String requestBody) throws Exception {
        mockMvc.perform(post("/api/comments")
                        .header("Monew-Request-User-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.articleId").exists());

        verifyNoInteractions(commentService);
    }
}

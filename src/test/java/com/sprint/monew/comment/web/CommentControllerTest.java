package com.sprint.monew.comment.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("댓글 목록 조회 요청을 서비스에 전달하고 커서 응답을 반환한다")
    void readAll() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-26T01:00:00Z");
        CursorRequest request = new CursorRequest(commentId.toString(), createdAt, 20);
        CommentDto comment =
                new CommentDto(commentId, articleId, UUID.randomUUID(), "작성자", "댓글 내용", 10L, true, createdAt);
        CursorPageResponse<CommentDto> response =
                new CursorPageResponse<>(List.of(comment), commentId.toString(), createdAt, 1, 7L, true);

        org.mockito.BDDMockito.given(
                        commentService.search(articleId, requesterId, "likeCount", SortDirection.DESC, request))
                .willReturn(response);

        mockMvc.perform(get("/api/comments")
                        .header("Monew-Request-User-ID", requesterId)
                        .param("articleId", articleId.toString())
                        .param("orderBy", "likeCount")
                        .param("direction", "DESC")
                        .param("cursor", commentId.toString())
                        .param("after", createdAt.toString())
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.content[0].userNickname").value("작성자"))
                .andExpect(jsonPath("$.content[0].likedByMe").value(true))
                .andExpect(jsonPath("$.nextCursor").value(commentId.toString()))
                .andExpect(jsonPath("$.nextAfter").value(createdAt.toString()))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(commentService).search(articleId, requesterId, "likeCount", SortDirection.DESC, request);
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 서비스 호출 전에 400을 반환한다")
    void rejectsMissingRequesterId() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("articleId", UUID.randomUUID().toString())
                        .param("orderBy", "createdAt")
                        .param("direction", "ASC")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("잘못된 페이지 크기는 서비스 호출 전에 400을 반환한다")
    void rejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .header("Monew-Request-User-ID", UUID.randomUUID())
                        .param("articleId", UUID.randomUUID().toString())
                        .param("orderBy", "createdAt")
                        .param("direction", "ASC")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }
}

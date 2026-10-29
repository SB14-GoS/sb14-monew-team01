package com.sprint.monew.comment.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.comment.domain.exception.CommentAccessDeniedException;
import com.sprint.monew.comment.domain.exception.CommentNotFoundException;
import com.sprint.monew.comment.domain.exception.InvalidCommentOrderByException;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @ParameterizedTest
    @ValueSource(strings = {"", "/hard"})
    @DisplayName("댓글 삭제 성공 시 204를 반환한다")
    void deleteReturnsNoContent(String suffix) throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/comments/{commentId}" + suffix, commentId).header("Monew-Request-User-ID", userId))
                .andExpect(status().isNoContent());

        if (suffix.isEmpty()) {
            verify(commentService).delete(commentId, userId);
        } else {
            verify(commentService).deleteHard(commentId, userId);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/hard"})
    @DisplayName("존재하지 않는 댓글 삭제 시 404를 반환한다")
    void deleteNotFound(String suffix) throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        if (suffix.isEmpty()) {
            willThrow(new CommentNotFoundException(commentId))
                    .given(commentService)
                    .delete(commentId, userId);
        } else {
            willThrow(new CommentNotFoundException(commentId))
                    .given(commentService)
                    .deleteHard(commentId, userId);
        }

        mockMvc.perform(delete("/api/comments/{commentId}" + suffix, commentId).header("Monew-Request-User-ID", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void 정상_댓글_등록은_201과_댓글_응답을_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-26T01:00:00Z");

        CommentDto response =
                new CommentDto(UUID.randomUUID(), articleId, userId, "작성자", "댓글입니다.", 0L, false, createdAt);

        given(commentService.register(articleId, userId, "댓글입니다.")).willReturn(response);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"articleId":"%s","userId":"%s","content":"댓글입니다."}
                            """.formatted(articleId, userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.content").value("댓글입니다."))
                .andExpect(jsonPath("$.likedByMe").value(false));

        verify(commentService).register(articleId, userId, "댓글입니다.");
    }

    @ParameterizedTest
    @MethodSource("missingArticleIdRequests")
    @DisplayName("articleId가 없거나 null이면 서비스 호출 전에 400을 반환한다")
    void rejectsMissingArticleId(String requestBody) throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.articleId").exists());

        verifyNoInteractions(commentService);
    }

    @ParameterizedTest
    @MethodSource("missingUserIdRequests")
    @DisplayName("userId가 없거나 null이면 서비스 호출 전에 400을 반환한다")
    void rejectsMissingUserId(String requestBody) throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userId").exists());

        verifyNoInteractions(commentService);
    }

    @ParameterizedTest
    @MethodSource("invalidContentRequests")
    @DisplayName("content가 비었거나 500자를 초과하면 서비스 호출 전에 400을 반환한다")
    void rejectsInvalidContent(String content) throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"articleId":"%s","userId":"%s","content":"%s"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), content)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.content").exists());

        verifyNoInteractions(commentService);
    }

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

        given(commentService.search(articleId, requesterId, "likeCount", SortDirection.DESC, request))
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

    @ParameterizedTest
    @ValueSource(strings = {"articleId", "orderBy", "direction"})
    @DisplayName("필수 댓글 목록 조회 파라미터가 없으면 서비스 호출 전에 400을 반환한다")
    void rejectsMissingRequiredReadParameter(String missingParameter) throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        var request = get("/api/comments")
                .header("Monew-Request-User-ID", requesterId)
                .param("limit", "10");

        if (!missingParameter.equals("articleId")) {
            request.param("articleId", articleId.toString());
        }
        if (!missingParameter.equals("orderBy")) {
            request.param("orderBy", "createdAt");
        }
        if (!missingParameter.equals("direction")) {
            request.param("direction", "ASC");
        }

        mockMvc.perform(request).andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("articleId가 UUID 형식이 아니면 서비스 호출 전에 400을 반환한다")
    void rejectsInvalidArticleId() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .header("Monew-Request-User-ID", UUID.randomUUID())
                        .param("articleId", "not-a-uuid")
                        .param("orderBy", "createdAt")
                        .param("direction", "ASC")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("direction이 지원하지 않는 값이면 서비스 호출 전에 400을 반환한다")
    void rejectsInvalidDirection() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .header("Monew-Request-User-ID", UUID.randomUUID())
                        .param("articleId", UUID.randomUUID().toString())
                        .param("orderBy", "createdAt")
                        .param("direction", "INVALID")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("orderBy가 지원하지 않는 값이면 400을 반환한다")
    void rejectsInvalidOrderBy() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        CursorRequest request = new CursorRequest(null, null, 10);
        willThrow(new InvalidCommentOrderByException("invalid"))
                .given(commentService)
                .search(articleId, requesterId, "invalid", SortDirection.ASC, request);

        mockMvc.perform(get("/api/comments")
                        .header("Monew-Request-User-ID", requesterId)
                        .param("articleId", articleId.toString())
                        .param("orderBy", "invalid")
                        .param("direction", "ASC")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());

        verify(commentService).search(articleId, requesterId, "invalid", SortDirection.ASC, request);
    }

    @Test
    @DisplayName("작성자는 JSON 본문으로 댓글을 수정할 수 있다")
    void update() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CommentDto response = new CommentDto(
                commentId, UUID.randomUUID(), userId, "작성자", "수정한 댓글", 3L, true, Instant.parse("2026-10-26T01:00:00Z"));
        given(commentService.update(commentId, userId, "수정한 댓글")).willReturn(response);

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Monew-Request-User-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정한 댓글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정한 댓글"))
                .andExpect(jsonPath("$.likeCount").value(3))
                .andExpect(jsonPath("$.likedByMe").value(true));

        verify(commentService).update(commentId, userId, "수정한 댓글");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a"})
    @DisplayName("유효하지 않은 수정 내용은 서비스 호출 전에 400을 반환한다")
    void rejectsInvalidUpdateContent(String content) throws Exception {
        String invalidContent = content.equals("a") ? "a".repeat(501) : content;

        mockMvc.perform(patch("/api/comments/{commentId}", UUID.randomUUID())
                        .header("Monew-Request-User-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\"}".formatted(invalidContent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.content").exists());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("수정 요청에 사용자 ID 헤더가 없으면 400을 반환한다")
    void rejectsMissingUpdateRequesterId() throws Exception {
        mockMvc.perform(patch("/api/comments/{commentId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정한 댓글\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정은 404를 반환한다")
    void updateNotFound() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        willThrow(new CommentNotFoundException(commentId)).given(commentService).update(commentId, userId, "수정한 댓글");

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Monew-Request-User-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정한 댓글\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("타인의 댓글 수정은 403을 반환한다")
    void updateAccessDenied() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        willThrow(new CommentAccessDeniedException(commentId, userId))
                .given(commentService)
                .update(commentId, userId, "수정한 댓글");

        mockMvc.perform(patch("/api/comments/{commentId}", commentId)
                        .header("Monew-Request-User-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정한 댓글\"}"))
                .andExpect(status().isForbidden());
    }

    private static Stream<String> missingArticleIdRequests() {
        UUID userId = UUID.randomUUID();
        return Stream.of("""
                {"userId":"%s","content":"댓글입니다."}
                """.formatted(userId), """
                {"articleId":null,"userId":"%s","content":"댓글입니다."}
                """.formatted(userId));
    }

    private static Stream<String> missingUserIdRequests() {
        UUID articleId = UUID.randomUUID();
        return Stream.of("""
                {"articleId":"%s","content":"댓글입니다."}
                """.formatted(articleId), """
                {"articleId":"%s","userId":null,"content":"댓글입니다."}
                """.formatted(articleId));
    }

    private static Stream<String> invalidContentRequests() {
        return Stream.of("", "   ", "a".repeat(501));
    }
}

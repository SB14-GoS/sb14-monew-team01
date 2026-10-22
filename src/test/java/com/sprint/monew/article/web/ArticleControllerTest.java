package com.sprint.monew.article.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.article.application.ArticleService;
import com.sprint.monew.article.domain.ArticleOrderBy;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ArticleService articleService;

    private final UUID userId = UUID.randomUUID();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("정렬과 페이지 크기를 생략하거나 빈 값으로 보내면 기본값을 사용한다")
    void usesDefaults(boolean emptyParams) throws Exception {
        given(articleService.search(any(), eq(userId)))
                .willReturn(new CursorPageResponse<>(List.of(), null, null, 0, 0, false));
        var request = get("/api/articles").header("Monew-Request-User-ID", userId);
        if (emptyParams) {
            request.param("orderBy", "").param("direction", "").param("limit", "");
        }

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(articleService)
                .search(
                        new ArticleSearchCondition(
                                null,
                                null,
                                null,
                                null,
                                null,
                                ArticleOrderBy.PUBLISH_DATE,
                                SortDirection.DESC,
                                new CursorRequest(null, null, 10)),
                        userId);
    }

    @Test
    @DisplayName("검색 조건과 커서 파라미터를 DTO로 바인딩해 서비스에 전달한다")
    void bindsAllSearchParameters() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        String cursor = "80|" + articleId;
        Instant after = Instant.parse("2026-10-21T00:00:00Z");
        ArticleDto article = new ArticleDto(
                articleId, ArticleSource.NAVER, "https://news.example/1", "경제 뉴스", after, "요약", 3L, 80L, true);
        given(articleService.search(any(), eq(userId)))
                .willReturn(new CursorPageResponse<>(List.of(article), cursor, after, 1, 53, true));

        mockMvc.perform(get("/api/articles")
                        .header("Monew-Request-User-ID", userId)
                        .param("keyword", "경제")
                        .param("interestId", interestId.toString())
                        .param("sourceIn", "NAVER,HANKYUNG")
                        .param("publishDateFrom", "2026-10-01T00:00:00")
                        .param("publishDateTo", "2026-10-22T00:00:00")
                        .param("orderBy", "viewCount")
                        .param("direction", "ASC")
                        .param("cursor", cursor)
                        .param("after", "2026-10-21T00:00:00Z")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(articleId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("경제 뉴스"))
                .andExpect(jsonPath("$.content[0].viewedByMe").value(true))
                .andExpect(jsonPath("$.nextCursor").value(cursor))
                .andExpect(jsonPath("$.nextAfter").value(after.toString()))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(53))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(articleService)
                .search(
                        new ArticleSearchCondition(
                                "경제",
                                interestId,
                                List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG),
                                LocalDateTime.parse("2026-10-01T00:00:00"),
                                LocalDateTime.parse("2026-10-22T00:00:00"),
                                ArticleOrderBy.VIEW_COUNT,
                                SortDirection.ASC,
                                new CursorRequest(cursor, Instant.parse("2026-10-21T00:00:00Z"), 20)),
                        userId);
    }

    @ParameterizedTest
    @EnumSource(ArticleOrderBy.class)
    @DisplayName("기존 정렬 파라미터 이름을 enum으로 변환한다")
    void bindsOrderByEnum(ArticleOrderBy orderBy) throws Exception {
        given(articleService.search(any(), eq(userId)))
                .willReturn(new CursorPageResponse<>(List.of(), null, null, 0, 0, false));
        mockMvc.perform(get("/api/articles")
                        .header("Monew-Request-User-ID", userId)
                        .param("orderBy", orderBy.getValue()))
                .andExpect(status().isOk());
        verify(articleService)
                .search(
                        new ArticleSearchCondition(
                                null,
                                null,
                                null,
                                null,
                                null,
                                orderBy,
                                SortDirection.DESC,
                                new CursorRequest(null, null, 10)),
                        userId);
    }

    @Test
    @DisplayName("사용자 ID 헤더가 없으면 서비스 호출 전에 400을 반환한다")
    void rejectsMissingUserId() throws Exception {
        mockMvc.perform(get("/api/articles")).andExpect(status().isBadRequest());

        verifyNoInteractions(articleService);
    }

    @ParameterizedTest
    @CsvSource({
        "limit, 0",
        "limit, 101",
        "limit, abc",
        "orderBy, invalid",
        "direction, invalid",
        "after, invalid",
        "interestId, invalid"
    })
    @DisplayName("잘못된 검색 파라미터는 서비스 호출 전에 400을 반환한다")
    void rejectsInvalidParameters(String name, String value) throws Exception {
        mockMvc.perform(get("/api/articles")
                        .header("Monew-Request-User-ID", userId)
                        .param(name, value))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(articleService);
    }
}

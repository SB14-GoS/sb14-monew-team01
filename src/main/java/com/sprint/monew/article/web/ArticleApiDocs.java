package com.sprint.monew.article.web;

import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.article.web.dto.ArticleSearchRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "뉴스 기사 관리")
public interface ArticleApiDocs {

    @Operation(summary = "뉴스 기사 단건 조회")
    ResponseEntity<ArticleDto> read(UUID articleId, UUID userId);

    @Operation(summary = "뉴스 기사 목록 조회")
    ResponseEntity<CursorPageResponse<ArticleDto>> search(ArticleSearchRequest request, UUID userId);
}

package com.sprint.monew.article.web;

import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.article.web.dto.ArticleSearchRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "뉴스 기사 관리")
public interface ArticleApiDocs {

    @Operation(summary = "뉴스 기사 단건 조회")
    ResponseEntity<ArticleDto> read(UUID articleId, UUID userId);

    @Operation(summary = "뉴스 기사 목록 조회")
    ResponseEntity<CursorPageResponse<ArticleDto>> search(ArticleSearchRequest request, UUID userId);

    @Operation(summary = "뉴스 기사 수집 출처 목록 조회", description = "서비스가 수집하는 전체 출처 목록을 반환합니다.")
    ResponseEntity<List<ArticleSource>> getSourceList();

    @Operation(summary = "뉴스 기사 논리 삭제")
    ResponseEntity<Void> delete(UUID articleId);

    @Operation(summary = "뉴스 기사 물리 삭제")
    ResponseEntity<Void> deleteHard(UUID articleId);
}

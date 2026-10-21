package com.sprint.monew.article.web;

import com.sprint.monew.article.web.dto.ArticleViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "뉴스 기사 관리")
public interface ArticleViewApiDocs {

    @Operation(summary = "기사 뷰 등록")
    ResponseEntity<ArticleViewDto> register(UUID articleId, UUID userId);
}

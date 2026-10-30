package com.sprint.monew.backup.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "뉴스 기사 관리")
public interface ArticleRestoreApiDocs {

    @Operation(summary = "뉴스 복구")
    ResponseEntity<List<ArticleRestoreResultDto>> restore(LocalDateTime from, LocalDateTime to);
}

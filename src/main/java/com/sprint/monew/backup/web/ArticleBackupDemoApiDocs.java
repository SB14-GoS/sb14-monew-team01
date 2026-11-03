package com.sprint.monew.backup.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;

@Tag(name = "시연용")
public interface ArticleBackupDemoApiDocs {

    @Operation(summary = "기사 수동 백업")
    ResponseEntity<String> backup(LocalDate date) throws Exception;
}

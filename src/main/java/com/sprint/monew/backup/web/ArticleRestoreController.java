package com.sprint.monew.backup.web;

import com.sprint.monew.backup.application.ArticleRestoreService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleRestoreController implements ArticleRestoreApiDocs {

    private final ArticleRestoreService articleRestoreService;

    @Override
    @GetMapping("/api/articles/restore")
    public ResponseEntity<List<ArticleRestoreResultDto>> restore(
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to) {
        List<ArticleRestoreResultDto> articleRestoreResultDtos = articleRestoreService.restore(from, to);
        return ResponseEntity.ok(articleRestoreResultDtos);
    }
}

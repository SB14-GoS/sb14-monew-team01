package com.sprint.monew.article.web;

import com.sprint.monew.article.application.ArticleViewService;
import com.sprint.monew.article.web.dto.ArticleViewDto;
import com.sprint.monew.global.web.RequestUserId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleViewController implements ArticleViewApiDocs {

    private final ArticleViewService articleViewService;

    @Override
    @PostMapping("/api/articles/{articleId}/article-views")
    public ResponseEntity<ArticleViewDto> register(@PathVariable UUID articleId, @RequestUserId UUID userId) {
        ArticleViewDto articleViewDto = articleViewService.register(articleId, userId);
        return ResponseEntity.ok(articleViewDto);
    }
}

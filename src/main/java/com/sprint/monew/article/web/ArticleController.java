package com.sprint.monew.article.web;

import com.sprint.monew.article.application.ArticleService;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.article.web.dto.ArticleSearchRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.web.RequestUserId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleController implements ArticleApiDocs {

    private final ArticleService articleService;

    @Override
    @GetMapping("/api/articles/{articleId}")
    public ResponseEntity<ArticleDto> read(@PathVariable UUID articleId, @RequestUserId UUID userId) {
        ArticleDto result = articleService.read(articleId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Override
    @GetMapping("/api/articles")
    public ResponseEntity<CursorPageResponse<ArticleDto>> search(
            @Valid @ModelAttribute ArticleSearchRequest request, @RequestUserId UUID userId) {
        CursorPageResponse<ArticleDto> result = articleService.search(request.toCondition(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Override
    @GetMapping("/api/articles/sources")
    public ResponseEntity<List<ArticleSource>> getSourceList() {
        return ResponseEntity.status(HttpStatus.OK).body(articleService.getSourceList());
    }

    @Override
    @DeleteMapping("/api/articles/{articleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID articleId) {
        articleService.delete(articleId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    @DeleteMapping("/api/articles/{articleId}/hard")
    public ResponseEntity<Void> deleteHard(@PathVariable UUID articleId) {
        articleService.deleteHard(articleId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

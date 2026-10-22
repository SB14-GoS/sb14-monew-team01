package com.sprint.monew.article.web;

import com.sprint.monew.article.application.ArticleService;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.article.web.dto.ArticleSearchRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.web.RequestUserId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleController implements ArticleApiDocs {

    private final ArticleService articleService;

    @Override
    @GetMapping("/api/articles")
    public ResponseEntity<CursorPageResponse<ArticleDto>> search(
            @Valid @ModelAttribute ArticleSearchRequest request, @RequestUserId UUID userId) {
        CursorPageResponse<ArticleDto> result = articleService.search(request.toCondition(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}

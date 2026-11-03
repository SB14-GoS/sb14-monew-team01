package com.sprint.monew.article.web;

import com.sprint.monew.article.application.ArticleCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleCollectController implements ArticleCollectApiDocs {

    private final ArticleCollectService articleCollectService;

    @PostMapping("/api/dev/articles/collect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void collect() {
        articleCollectService.save();
    }
}

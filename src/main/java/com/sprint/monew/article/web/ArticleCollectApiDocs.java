package com.sprint.monew.article.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "시연용")
public interface ArticleCollectApiDocs {

    @Operation(summary = "기사 수동 수집")
    void collect();
}

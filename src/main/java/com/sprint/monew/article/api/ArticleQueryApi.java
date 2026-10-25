package com.sprint.monew.article.api;

import java.util.UUID;

public interface ArticleQueryApi {

    // 댓글 등록시 기사 검증을 위해
    boolean exists(UUID articleId);

    String getTitle(UUID articleId);
}

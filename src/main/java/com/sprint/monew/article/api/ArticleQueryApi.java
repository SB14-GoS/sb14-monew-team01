package com.sprint.monew.article.api;

import java.util.UUID;

public interface ArticleQueryApi {

    boolean exists(UUID articleId);

    String getTitle(UUID articleId);
}

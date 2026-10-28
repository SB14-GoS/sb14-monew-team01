package com.sprint.monew.article.api;

import java.util.UUID;

public interface ArticleCommandApi {

    void increaseCommentCount(UUID articleId);

    void decreaseCommentCount(UUID articleId);
}

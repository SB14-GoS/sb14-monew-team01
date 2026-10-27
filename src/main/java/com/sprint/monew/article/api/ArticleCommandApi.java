package com.sprint.monew.article.api;

import java.util.UUID;

public interface ArticleCommandApi {

    void increaseCommentCount(UUID articleId);

    /** 활성 기사의 댓글 수를 1 감소시킨다. 이미 0이면 유지한다. */
    void decreaseCommentCount(UUID articleId);
}

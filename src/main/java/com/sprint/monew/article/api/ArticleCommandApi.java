package com.sprint.monew.article.api;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** 댓글 등 다른 모듈이 기사의 집계 값을 변경할 때 사용하는 공개 계약입니다. */
@Component
public interface ArticleCommandApi {

    void increaseCommentCount(UUID articleId);
}

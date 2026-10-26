package com.sprint.monew.article.api;

import java.util.UUID;

public interface ArticleCleanupApi {

    /**
     * 기사 물리 삭제 전에 관련 데이터를 정리한다.
     * 논리 삭제된 데이터도 포함하며, 대상이 없어도 정상 종료한다.
     * 호출자의 트랜잭션에 참여하고 실패 시 예외를 전파한다.
     */
    void deleteAllByArticleId(UUID articleId);
}

package com.sprint.monew.article.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ArticleRestoreApi {

    // contains 확인 속도를 위해 반환값 Set 사용
    Set<String> findExistingSourceUrls(List<String> sourceUrls);

    List<UUID> restore(List<ArticleRestoreCommand> commands);
}

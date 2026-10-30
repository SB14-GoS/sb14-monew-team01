package com.sprint.monew.backup.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResultDto(Instant restoreDate, List<UUID> restoredArticleIds, long restoredArticleCount) {
    public static ArticleRestoreResultDto of(Instant restoreDate, List<UUID> restoredArticleIds) {
        return new ArticleRestoreResultDto(restoreDate, restoredArticleIds, restoredArticleIds.size());
    }
}

package com.sprint.monew.backup.application;

import com.sprint.monew.article.api.ArticleBackupDto;
import com.sprint.monew.article.api.ArticleRestoreCommand;
import java.time.Instant;
import java.util.UUID;

public record ArticleBackupRecord(
        UUID id, String title, Instant publishedDate, String summary, String source, String sourceUrl) {

    public static ArticleBackupRecord from(ArticleBackupDto dto) {
        return new ArticleBackupRecord(
                dto.id(), dto.title(), dto.publishedDate(), dto.summary(), dto.source(), dto.sourceUrl());
    }

    public ArticleRestoreCommand toRestoreCommand() {
        return new ArticleRestoreCommand(title, publishedDate, summary, source, sourceUrl);
    }
}

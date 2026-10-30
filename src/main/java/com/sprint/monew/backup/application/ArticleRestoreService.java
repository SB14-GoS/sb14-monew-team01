package com.sprint.monew.backup.application;

import com.sprint.monew.article.api.ArticleRestoreApi;
import com.sprint.monew.article.api.ArticleRestoreCommand;
import com.sprint.monew.backup.application.port.FileStorage;
import com.sprint.monew.backup.domain.BackupKey;
import com.sprint.monew.backup.domain.exception.BackupSerializationFailedException;
import com.sprint.monew.backup.domain.exception.InvalidRestoreRangeException;
import com.sprint.monew.backup.web.ArticleRestoreResultDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ArticleRestoreService {

    private final FileStorage fileStorage;
    private final ArticleRestoreApi articleRestoreApi;
    private final JsonMapper jsonMapper;

    public List<ArticleRestoreResultDto> restore(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new InvalidRestoreRangeException(from, to);
        }
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();
        if (ChronoUnit.DAYS.between(fromDate, toDate) >= 31) {
            throw new InvalidRestoreRangeException(from, to);
        }

        List<ArticleRestoreResultDto> results = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            results.add(restoreOneDay(date));
        }

        return results;
    }

    private ArticleRestoreResultDto restoreOneDay(LocalDate date) {
        Optional<byte[]> file = fileStorage.get(BackupKey.of(date));
        if (file.isEmpty()) {
            return ArticleRestoreResultDto.of(BackupKey.startOf(date), List.of());
        }

        List<ArticleBackupRecord> backupRecords = readRecords(file.get());

        Set<String> existing = articleRestoreApi.findExistingSourceUrls(
                backupRecords.stream().map(ArticleBackupRecord::sourceUrl).toList());

        List<ArticleRestoreCommand> missing = backupRecords.stream()
                .filter(record -> !existing.contains(record.sourceUrl()))
                .map(ArticleBackupRecord::toRestoreCommand)
                .toList();

        List<UUID> restoredIds = missing.isEmpty() ? List.of() : articleRestoreApi.restore(missing);

        return ArticleRestoreResultDto.of(BackupKey.startOf(date), restoredIds);
    }

    private List<ArticleBackupRecord> readRecords(byte[] content) {
        try {
            return jsonMapper.readValue(content, new TypeReference<List<ArticleBackupRecord>>() {});
        } catch (JacksonException exception) {
            throw new BackupSerializationFailedException(exception);
        }
    }
}

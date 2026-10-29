package com.sprint.monew.backup.application;

import com.sprint.monew.article.api.ArticleBackupApi;
import com.sprint.monew.backup.application.port.FileStorage;
import com.sprint.monew.backup.domain.BackupKey;
import com.sprint.monew.backup.domain.exception.BackupSerializationFailedException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleBackupService {

    private final ArticleBackupApi articleBackupApi;
    private final FileStorage fileStorage;
    private final JsonMapper jsonMapper;

    public int backup(LocalDate date) {
        List<ArticleBackupRecord> records =
                articleBackupApi.findAllCreatedBetween(BackupKey.startOf(date), BackupKey.endOf(date)).stream()
                        .map(ArticleBackupRecord::from)
                        .toList();

        String key = BackupKey.of(date);
        fileStorage.put(key, toJson(records));
        log.info("기사 백업 완료: date={}, count={}, key={}", date, records.size(), key);
        return records.size();
    }

    private byte[] toJson(List<ArticleBackupRecord> records) {
        try {
            return jsonMapper.writeValueAsBytes(records);
        } catch (JacksonException exception) {
            throw new BackupSerializationFailedException(exception);
        }
    }
}

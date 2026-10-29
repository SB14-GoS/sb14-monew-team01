package com.sprint.monew.backup.domain.exception;

import com.sprint.monew.backup.domain.BackupErrorCode;
import java.util.Map;

public class BackupStorageFailedException extends BackupException {

    public BackupStorageFailedException(String key, Throwable cause) {
        super(BackupErrorCode.BACKUP_STORAGE_FAILED, Map.of("key", key), cause);
    }
}

package com.sprint.monew.backup.domain.exception;

import com.sprint.monew.activity.domain.exception.ActivityException;
import com.sprint.monew.backup.domain.BackupErrorCode;
import java.util.Map;

public class BackupStorageFailedException extends ActivityException {

    public BackupStorageFailedException(String key) {
        super(BackupErrorCode.BACKUP_STORAGE_FAILED, Map.of("key", key));
    }
}

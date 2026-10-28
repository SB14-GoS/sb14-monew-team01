package com.sprint.monew.backup.domain.exception;

import com.sprint.monew.activity.domain.exception.ActivityException;
import com.sprint.monew.backup.domain.BackupErrorCode;
import java.util.Map;

public class BackupSerializationFailedException extends ActivityException {

    public BackupSerializationFailedException() {
        super(BackupErrorCode.BACKUP_SERIALIZATION_FAILED, Map.of());
    }
}

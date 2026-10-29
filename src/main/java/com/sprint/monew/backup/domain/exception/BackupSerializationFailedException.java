package com.sprint.monew.backup.domain.exception;

import com.sprint.monew.backup.domain.BackupErrorCode;
import java.util.Map;

public class BackupSerializationFailedException extends BackupException {

    public BackupSerializationFailedException(Throwable cause) {
        super(BackupErrorCode.BACKUP_SERIALIZATION_FAILED, Map.of(), cause);
    }
}

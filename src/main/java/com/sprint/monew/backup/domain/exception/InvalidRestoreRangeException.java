package com.sprint.monew.backup.domain.exception;

import com.sprint.monew.backup.domain.BackupErrorCode;
import java.time.LocalDateTime;
import java.util.Map;

public class InvalidRestoreRangeException extends BackupException {

    public InvalidRestoreRangeException(LocalDateTime from, LocalDateTime to) {
        super(BackupErrorCode.INVALID_RESTORE_RANGE, Map.of("from", from, "to", to));
    }
}

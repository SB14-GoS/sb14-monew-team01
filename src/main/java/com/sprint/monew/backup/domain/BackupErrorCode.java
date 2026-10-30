package com.sprint.monew.backup.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BackupErrorCode implements ErrorCode {
    BACKUP_STORAGE_FAILED(Level.ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "백업 파일 저장소 처리에 실패했습니다."),
    BACKUP_SERIALIZATION_FAILED(Level.ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "백업 파일 변환에 실패했습니다."),
    INVALID_RESTORE_RANGE(Level.WARN, HttpStatus.BAD_REQUEST, "복구 기간이 올바르지 않습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}

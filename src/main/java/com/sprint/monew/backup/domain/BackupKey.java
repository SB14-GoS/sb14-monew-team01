package com.sprint.monew.backup.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class BackupKey {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String PREFIX = "articles/";
    private static final String EXTENSION = ".json";

    private BackupKey() {}

    // 파일명 변경, articles/날짜.json
    public static String of(LocalDate date) {
        return PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + EXTENSION;
    }

    public static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    public static Instant endOf(LocalDate date) {
        return startOf(date.plusDays(1));
    }
}

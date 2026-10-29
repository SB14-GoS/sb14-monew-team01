package com.sprint.monew.backup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BackupKeyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);

    @Test
    @DisplayName("키는 articles/yyyy-MM-dd.json 형식이다")
    void keyFormat() {
        assertThat(BackupKey.of(DATE)).isEqualTo("articles/2026-10-28.json");
    }

    @Test
    @DisplayName("기간은 KST 자정부터 다음날 KST 자정까지다")
    void rangeIsKstDay() {
        assertThat(BackupKey.startOf(DATE)).isEqualTo(Instant.parse("2026-10-27T15:00:00Z"));
        assertThat(BackupKey.endOf(DATE)).isEqualTo(Instant.parse("2026-10-28T15:00:00Z"));
    }
}

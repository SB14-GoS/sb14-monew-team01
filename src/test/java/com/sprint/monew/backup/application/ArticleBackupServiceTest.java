package com.sprint.monew.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleBackupApi;
import com.sprint.monew.article.api.ArticleBackupDto;
import com.sprint.monew.backup.application.port.FileStorage;
import com.sprint.monew.backup.domain.BackupKey;
import com.sprint.monew.backup.domain.exception.BackupStorageFailedException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class ArticleBackupServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private ArticleBackupApi articleBackupApi;
    private FileStorage fileStorage;
    private ArticleBackupService articleBackupService;

    @BeforeEach
    void setUp() {
        articleBackupApi = mock(ArticleBackupApi.class);
        fileStorage = mock(FileStorage.class);
        articleBackupService = new ArticleBackupService(articleBackupApi, fileStorage, jsonMapper);
    }

    @Test
    @DisplayName("그날 기사를 JSON으로 만들어 날짜 키로 저장하고 건수를 반환한다")
    void backupStoresArticlesOfTheDay() {
        ArticleBackupDto article = new ArticleBackupDto(
                UUID.randomUUID(), "제목", Instant.parse("2026-10-28T01:00:00Z"), "요약", "NAVER", "https://a.com/1");
        given(articleBackupApi.findAllCreatedBetween(BackupKey.startOf(DATE), BackupKey.endOf(DATE)))
                .willReturn(List.of(article));

        int count = articleBackupService.backup(DATE);

        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorage).put(eq("articles/2026-10-28.json"), content.capture());
        List<ArticleBackupRecord> saved = jsonMapper.readValue(content.getValue(), new TypeReference<>() {});
        assertThat(count).isEqualTo(1);
        assertThat(saved).containsExactly(ArticleBackupRecord.from(article));
    }

    @Test
    @DisplayName("기사가 없어도 빈 배열 파일을 저장한다")
    void backupStoresEmptyArray() {
        given(articleBackupApi.findAllCreatedBetween(any(), any())).willReturn(List.of());

        int count = articleBackupService.backup(DATE);

        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorage).put(anyString(), content.capture());
        assertThat(count).isZero();
        assertThat(new String(content.getValue())).isEqualTo("[]");
    }

    @Test
    @DisplayName("저장소 실패는 그대로 던진다")
    void storageFailurePropagates() {
        given(articleBackupApi.findAllCreatedBetween(any(), any())).willReturn(List.of());
        willThrow(new BackupStorageFailedException("articles/2026-10-28.json", new RuntimeException()))
                .given(fileStorage)
                .put(anyString(), any());

        assertThatThrownBy(() -> articleBackupService.backup(DATE)).isInstanceOf(BackupStorageFailedException.class);
    }
}

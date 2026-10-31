package com.sprint.monew.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.article.api.ArticleRestoreApi;
import com.sprint.monew.backup.application.port.FileStorage;
import com.sprint.monew.backup.domain.BackupKey;
import com.sprint.monew.backup.domain.exception.InvalidRestoreRangeException;
import com.sprint.monew.backup.web.ArticleRestoreResultDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ArticleRestoreServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);
    private static final LocalDateTime FROM = DATE.atStartOfDay();
    private static final LocalDateTime TO = DATE.atTime(23, 59, 59);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private FileStorage fileStorage;
    private ArticleRestoreApi articleRestoreApi;
    private ArticleRestoreService articleRestoreService;

    @BeforeEach
    void setUp() {
        fileStorage = mock(FileStorage.class);
        articleRestoreApi = mock(ArticleRestoreApi.class);
        articleRestoreService = new ArticleRestoreService(fileStorage, articleRestoreApi, jsonMapper);
    }

    @Test
    @DisplayName("백업 파일이 없는 날은 0건으로 응답한다")
    void restoreReturnsZeroWhenBackupMissing() {
        given(fileStorage.get(BackupKey.of(DATE))).willReturn(Optional.empty());

        List<ArticleRestoreResultDto> results = articleRestoreService.restore(FROM, TO);

        assertThat(results).containsExactly(new ArticleRestoreResultDto(BackupKey.startOf(DATE), List.of(), 0));
        verify(articleRestoreApi, never()).restore(any());
    }

    @Test
    @DisplayName("DB에 이미 있는 기사는 빼고 없는 기사만 복구한다")
    void restoreOnlyMissingArticles() {
        UUID interestId = UUID.randomUUID();
        ArticleBackupRecord existing = record("https://a.com/1", List.of());
        ArticleBackupRecord missing = record("https://a.com/2", List.of(interestId));
        given(fileStorage.get(BackupKey.of(DATE)))
                .willReturn(Optional.of(jsonMapper.writeValueAsBytes(List.of(existing, missing))));
        given(articleRestoreApi.findExistingSourceUrls(List.of("https://a.com/1", "https://a.com/2")))
                .willReturn(Set.of("https://a.com/1"));
        UUID restoredId = UUID.randomUUID();
        given(articleRestoreApi.restore(List.of(missing.toRestoreCommand()))).willReturn(List.of(restoredId));

        List<ArticleRestoreResultDto> results = articleRestoreService.restore(FROM, TO);

        assertThat(results)
                .containsExactly(new ArticleRestoreResultDto(BackupKey.startOf(DATE), List.of(restoredId), 1));
        verify(articleRestoreApi).restore(List.of(missing.toRestoreCommand()));
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 예외를 던진다")
    void restoreThrowsWhenRangeReversed() {
        assertThatThrownBy(() -> articleRestoreService.restore(TO.plusDays(1), FROM))
                .isInstanceOf(InvalidRestoreRangeException.class);
    }

    private ArticleBackupRecord record(String sourceUrl, List<UUID> interestIds) {
        return new ArticleBackupRecord(
                UUID.randomUUID(), interestIds, "제목", Instant.parse("2026-10-28T01:00:00Z"), "요약", "NAVER", sourceUrl);
    }
}

package com.sprint.monew.global.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class LogUploadTest {

    @TempDir
    Path tempDir;

    private S3Client s3Client;
    private LogUploadService logUploadService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        logUploadService = new LogUploadService(s3Client, "test-log-bucket", tempDir.toString());
    }

    @Test
    @DisplayName("날짜별 로그 파일이 존재하면 올바른 버킷과 키 경로로 S3에 업로드한다")
    void uploadLogSuccess() throws IOException {
        // given: 임시 폴더에 가짜 로그 파일 생성
        LocalDate targetDate = LocalDate.of(2026, 11, 1);
        Path logFile = tempDir.resolve("monew.2026-11-01.log");
        Files.writeString(logFile, "테스트 로그 내용입니다.");

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        boolean result = logUploadService.uploadLogByDate(targetDate);

        // then: 업로드 성공 여부와 S3 요청 파라미터(버킷명, 키, 컨텐츠 타입) 검증
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        assertThat(result).isTrue();
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-log-bucket");
        assertThat(capturedRequest.key()).isEqualTo("logs/monew.2026-11-01.log");
        assertThat(capturedRequest.contentType()).isEqualTo("text/plain; charset=utf-8");
    }

    @Test
    @DisplayName("대상 날짜가 null이면 S3 업로드를 수행하지 않고 false를 반환한다")
    void uploadLogWhenTargetDateIsNull() {
        // when
        boolean result = logUploadService.uploadLogByDate(null);

        // then
        assertThat(result).isFalse();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("업로드할 로그 파일이 존재하지 않으면 S3 업로드를 수행하지 않는다")
    void uploadLogWhenFileNotExists() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 11, 1);

        // when
        boolean result = logUploadService.uploadLogByDate(targetDate);

        // then
        assertThat(result).isFalse();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 로그 버킷 이름이 비어 있으면 업로드를 건너뛴다")
    void uploadLogWhenBucketIsBlank() {
        // given
        LogUploadService emptyBucketService = new LogUploadService(s3Client, "   ", tempDir.toString());

        // when
        boolean result = emptyBucketService.uploadLogByDate(LocalDate.of(2026, 11, 1));

        // then
        assertThat(result).isFalse();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 업로드 중 예외가 발생하면 IllegalStateException을 던진다")
    void uploadLogWhenS3ExceptionOccurs() throws IOException {
        // given
        LocalDate targetDate = LocalDate.of(2026, 11, 1);
        Path logFile = tempDir.resolve("monew.2026-11-01.log");
        Files.writeString(logFile, "테스트 로그 내용입니다.");

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(new RuntimeException("S3 네트워크 오류"));

        // when & then: 예외가 정상적으로 던져지고, 우리가 던진 메시지와 원인 예외가 잘 담겨있는지 검증
        assertThatThrownBy(() -> logUploadService.uploadLogByDate(targetDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3 로그 업로드 실패")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("스케줄러 실행 시 Clock 기준 어제 날짜의 로그 업로드를 호출한다")
    void schedulerUploadsYesterdayLog() {
        // given: 서울 시간 기준 2026-11-02 00:05:00으로 고정된 Clock 설정
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-01T15:05:00Z"), ZoneId.of("Asia/Seoul"));
        LogUploadService mockService = mock(LogUploadService.class);
        LogUploadScheduler scheduler = new LogUploadScheduler(mockService, fixedClock);

        // when
        scheduler.uploadYesterdayLog();

        // then: 어제 날짜인 2026-11-01로 호출되었는지 검증
        verify(mockService).uploadLogByDate(LocalDate.of(2026, 11, 1));
    }
}

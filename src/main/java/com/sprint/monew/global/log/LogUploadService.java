package com.sprint.monew.global.log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
public class LogUploadService {
    private final S3Client s3Client;
    private final String logBucket;
    private final String logDir;

    public LogUploadService(
            S3Client s3Client,
            @Value("${monew.storage.s3.log-bucket:}") String logBucket,
            @Value("${monew.log.dir:./logs}") String logDir) {
        this.s3Client = s3Client;
        this.logBucket = logBucket;
        this.logDir = logDir;
    }

    public boolean uploadLogByDate(LocalDate targetDate) {
        if (targetDate == null) {
            log.warn("업로드 대상 날짜가 지정되지 않아 업로드를 건너뜁니다.");
            return false;
        }

        if (!StringUtils.hasText(logBucket)) {
            log.warn("S3 로그 버킷이 설정되지 않아 업로드를 건너뜁니다.");
            return false;
        }

        String fileName = String.format("monew.%s.log", targetDate);
        Path logFilePath = Paths.get(logDir, fileName);

        if (!Files.exists(logFilePath)) {
            log.info("업로드할 로그 파일이 존재하지 않습니다: {}", logFilePath);
            return false;
        }

        String s3Key = "logs/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(logBucket)
                    .key(s3Key)
                    .contentType("text/plain; charset=utf-8")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(logFilePath));
            log.info("S3 로그 업로드 완료: s3://{}/{}", logBucket, s3Key);
            return true;
        } catch (Exception e) {
            log.error("S3 로그 업로드 실패 (대상 파일: {})", logFilePath, e);
            return false;
        }
    }
}

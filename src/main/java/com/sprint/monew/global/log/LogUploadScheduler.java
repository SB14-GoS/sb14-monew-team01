package com.sprint.monew.global.log;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogUploadScheduler {
    private final LogUploadService logUploadService;
    private final Clock clock;

    // 00시 05분에 전날의 로그 파일 업로드
    @Scheduled(cron = "${monew.log.upload-cron:0 5 0 * * *}")
    public void uploadYesterdayLog() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        log.info("일일 로그 S3 업로드 스케줄러 실행 (날짜: {})", yesterday);
        logUploadService.uploadLogByDate(yesterday);
    }
}

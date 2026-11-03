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
    @Scheduled(cron = "${monew.log.upload-cron:0 5 0 * * *}", zone = "Asia/Seoul")
    public void uploadYesterdayLog() {
        for (int i = 1; i <= 7; i++) {
            LocalDate targetDate = LocalDate.now(clock).minusDays(i);
            try {
                // 파일이 없으면 서비스 단에서 false 반환 후 스킵됨
                logUploadService.uploadLogByDate(targetDate);
            } catch (Exception e) {
                log.warn("날짜 {} 로그 업로드 실패. 다음 스케줄러 실행 시 재시도됩니다. 원인: {}", targetDate, e.getMessage());
            }
        }
    }
}

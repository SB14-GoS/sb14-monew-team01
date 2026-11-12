package com.sprint.monew.activity.application;

import com.sprint.monew.activity.application.ActivityOutboxRelay.RelayResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "monew.activity.outbox",
        name = "relay-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ActivityOutboxScheduler {

    /** 한 번 호출에 처리할 최대 배치 수. 백로그가 많아도 스케줄러 스레드를 오래 붙잡지 않고 다음 주기로 넘긴다. */
    static final int MAX_BATCHES_PER_RUN = 10;

    private final ActivityOutboxRelay relay;

    /** 쌓인 이벤트가 있으면 배치를 연달아 돌린다 (최대 {@value #MAX_BATCHES_PER_RUN}번). */
    @Scheduled(fixedDelayString = "${monew.activity.outbox.poll-delay-ms:1000}")
    public void relay() {
        RelayResult result;
        int batches = 0;
        do {
            result = relay.relayBatch();
            batches++;
        } while (result.hasMore() && batches < MAX_BATCHES_PER_RUN);
    }

    @Scheduled(cron = "${monew.activity.outbox.purge-cron:0 30 4 * * *}", zone = "Asia/Seoul")
    public void purge() {
        int deleted = relay.purgeProcessed();
        log.info("활동 내역 outbox 정리 - 삭제: {}", deleted);
    }
}

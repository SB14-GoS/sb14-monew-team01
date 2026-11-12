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

    private final ActivityOutboxRelay relay;

    /** 쌓인 이벤트가 있으면 배치를 연달아 돌려 비운다. */
    @Scheduled(fixedDelayString = "${monew.activity.outbox.poll-delay-ms:1000}")
    public void relay() {
        RelayResult result;
        do {
            result = relay.relayBatch();
        } while (result.hasMore());
    }

    @Scheduled(cron = "${monew.activity.outbox.purge-cron:0 30 4 * * *}", zone = "Asia/Seoul")
    public void purge() {
        int deleted = relay.purgeProcessed();
        log.info("활동 내역 outbox 정리 - 삭제: {}", deleted);
    }
}

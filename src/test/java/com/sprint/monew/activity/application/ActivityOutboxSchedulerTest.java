package com.sprint.monew.activity.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.monew.activity.application.ActivityOutboxRelay.RelayResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityOutboxSchedulerTest {

    @Mock
    ActivityOutboxRelay relay;

    @InjectMocks
    ActivityOutboxScheduler scheduler;

    @Test
    @DisplayName("배치가 남아 있으면 이어서 돌린다")
    void continuesWhileHasMore() {
        given(relay.relayBatch())
                .willReturn(new RelayResult(100, 0, true, false))
                .willReturn(new RelayResult(3, 0, false, false));

        scheduler.relay();

        verify(relay, times(2)).relayBatch();
    }

    @Test
    @DisplayName("백로그가 계속 남아도 한 번 호출에 최대 배치 수까지만 돌린다")
    void stopsAtMaxBatches() {
        given(relay.relayBatch()).willReturn(new RelayResult(100, 0, true, false));

        scheduler.relay();

        verify(relay, times(ActivityOutboxScheduler.MAX_BATCHES_PER_RUN)).relayBatch();
    }
}

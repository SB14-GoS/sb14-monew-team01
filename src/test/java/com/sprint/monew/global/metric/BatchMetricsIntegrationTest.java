package com.sprint.monew.global.metric;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class BatchMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BatchMetrics batchMetrics;

    @Test
    @DisplayName("배치 커스텀 메트릭 기록 후 Actuator 엔드포인트(/actuator/metrics)로 조회할 수 있다")
    void actuatorExposesBatchCustomMetrics() throws Exception {
        // given: 커스텀 메트릭 기록
        String jobName = "integrationTestJob";
        batchMetrics.incrementCollected(jobName, 12);
        batchMetrics.incrementFailed(jobName, 3);
        batchMetrics.recordDuration(jobName, "COMPLETED", Duration.ofSeconds(5));

        // when & then 1: 수집 건수 메트릭 엔드포인트 응답 확인
        mockMvc.perform(get("/actuator/metrics/" + BatchMetrics.COLLECTED_COUNT_METRIC)
                        .param("tag", "job:" + jobName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BatchMetrics.COLLECTED_COUNT_METRIC))
                .andExpect(jsonPath("$.measurements[0].value").value(12.0));

        // when & then 2: 실패 건수 메트릭 엔드포인트 응답 확인
        mockMvc.perform(get("/actuator/metrics/" + BatchMetrics.FAILED_COUNT_METRIC)
                        .param("tag", "job:" + jobName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BatchMetrics.FAILED_COUNT_METRIC))
                .andExpect(jsonPath("$.measurements[0].value").value(3.0));

        // when & then 3: 소요 시간 메트릭 엔드포인트 응답 확인
        mockMvc.perform(get("/actuator/metrics/" + BatchMetrics.DURATION_METRIC)
                        .param("tag", "job:" + jobName)
                        .param("tag", "status:COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BatchMetrics.DURATION_METRIC));
    }
}

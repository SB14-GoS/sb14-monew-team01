package com.sprint.monew.global.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sprint.monew.support.IntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        Duration recordedDuration = Duration.ofSeconds(5);
        batchMetrics.incrementCollected(jobName, 12);
        batchMetrics.incrementFailed(jobName, 3);
        batchMetrics.recordDuration(jobName, "COMPLETED", recordedDuration);

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

        // when & then 3: 소요 시간 메트릭 엔드포인트 응답(COUNT 및 baseUnit 기준 TOTAL_TIME) 확인
        MvcResult durationResult = mockMvc.perform(get("/actuator/metrics/" + BatchMetrics.DURATION_METRIC)
                        .param("tag", "job:" + jobName)
                        .param("tag", "status:COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BatchMetrics.DURATION_METRIC))
                .andExpect(jsonPath("$.measurements[?(@.statistic == 'COUNT')].value", hasItem(1.0)))
                .andReturn();

        String responseBody = durationResult.getResponse().getContentAsString();
        String baseUnit = JsonPath.read(responseBody, "$.baseUnit");
        double expectedTotalTime =
                TimeUnit.valueOf(baseUnit.toUpperCase(Locale.ROOT)).convert(recordedDuration);

        List<Number> totalTimes = JsonPath.read(responseBody, "$.measurements[?(@.statistic == 'TOTAL_TIME')].value");
        assertThat(totalTimes)
                .singleElement()
                .satisfies(value -> assertThat(value.doubleValue()).isEqualTo(expectedTotalTime));
    }
}
